// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.hulylabs.intellij.plugins.completion.providers.supermaven.messages.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

private val LOG = Logger.getInstance(SupermavenAgent::class.java)

data class SupermavenCompletionState(
  val entryId: Int,
  val prefixOffset: Int,
  var text: String = "",
  var dedent: String = "",
  var end: Boolean = false,
)

enum class SupermavenAccountStatus {
  UNKNOWN, NEEDS_ACTIVATION, READY;
}

class SupermavenAgent(agentPath: Path) {
  private val process: Process

  private val stdout: BufferedReader
  private val stderr: BufferedReader
  private val stdin: BufferedWriter

  private val outputMessages = ArrayBlockingQueue<SupermavenOutboundMessage>(2048)
  private val incomingMessages = ArrayBlockingQueue<SupermavenMessage>(2048)

  private val json = Json {
    ignoreUnknownKeys = true
  }
  private var lastDrainTime: Long = 0

  internal val states: TreeMap<Long, SupermavenCompletionState> = TreeMap()
  internal var accountStatus: SupermavenAccountStatus = SupermavenAccountStatus.UNKNOWN
  internal var serviceTier: String? = null
  internal var activationUrl: String? = null
  internal var newStateId = 0L

  init {
    val processBuilder = ProcessBuilder(agentPath.toString(), "stdio")
    processBuilder.environment()["SM_LOG_PATH"] = agentPath.parent.resolve("sm-agent.log").toString()
    process = processBuilder.start()
    stdout = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
    stderr = BufferedReader(InputStreamReader(process.errorStream, Charsets.UTF_8))
    stdin = process.outputWriter(Charsets.UTF_8)
    thread {
      while (true) {
        val line = outputMessages.take()
        try {
          stdin.write(line.toJson() + '\n')
          stdin.flush()
        }
        catch (e: IOException) {
          LOG.error("error in process stdin: ", e)
          break
        }
      }
    }
    thread {
      while (true) {
        val str = try {
          stdout.readLine()
        }
        catch (e: IOException) {
          break
        }
        var line = str ?: continue
        if (!line.startsWith("SM-MESSAGE")) {
          LOG.warn("stdout: $line")
          continue
        } // remove prefix
        line = line.substring(10)
        try {
          incomingMessages.put(json.decodeFromString<SupermavenMessage>(line))
        }
        catch (e: Exception) {
          LOG.error("error in process message (line=$line): ", e)
        }
        val now = System.currentTimeMillis()
        if (now - lastDrainTime > 1000) {
          lastDrainTime = now
          ApplicationManager.getApplication().invokeLater {
            drainOutput()
          }
        }
      }
    }
    thread {
      while (true) {
        val str = try {
          stderr.readLine()
        }
        catch (e: IOException) {
          break
        }
        LOG.warn("stderr: ${str ?: continue}")
      }
    }
    accountStatus = SupermavenAccountStatus.READY
  }

  fun drainOutput() {
    LOG.info("draining output")
    while (process.isAlive) {
      val message = incomingMessages.poll() ?: break
      handleMessage(message)
    }
  }

  private fun handleMessage(message: SupermavenMessage) {
    when (message) {
      is SupermavenResponseMessage -> {
        states[message.stateId.toLong()]?.apply {
          message.items?.forEach { item ->
            when (item.kind) {
              ResponseItemKind.TEXT -> text += item.text
              ResponseItemKind.DEDENT -> dedent += item.text
              ResponseItemKind.END -> end = true
              else -> {}
            }
          }
        }
      }
      is SupermavenActivationRequestMessage -> {
        accountStatus = SupermavenAccountStatus.NEEDS_ACTIVATION
        activationUrl = message.activateUrl
        val settings: SupermavenSettings = ApplicationManager.getApplication().service()
        // on first run use free version activation automatically
        if (!settings.state.firstActivation) {
          settings.state.firstActivation = true
          LOG.info("sending free activation request")
          ApplicationManager.getApplication().invokeLater {
            send(SupermavenFreeActivationMessage())
          }
        }
      }
      is SupermavenActivationSuccessMessage -> {
        accountStatus = SupermavenAccountStatus.READY
      }
      is SupermavenServiceTierMessage -> {
        serviceTier = message.serviceTier
        accountStatus = SupermavenAccountStatus.READY
      }
      is SupermavenPassthroughMessage -> {
        handleMessage(message.passthrough)
      }
      else -> {
        LOG.warn("unhandled message: $message")
      }
    }
  }

  fun newCompletionState(entryId: Int, cursorOffset: Int) {
    newStateId++
    states[newStateId] = SupermavenCompletionState(entryId, cursorOffset)
    if (states.size > 1000) {
      states.remove(states.firstKey())
    }
  }

  fun isAlive(): Boolean {
    return process.isAlive
  }

  fun send(msg: SupermavenOutboundMessage): Boolean {
    return outputMessages.offer(msg)
  }

  fun destroy() {
    process.destroy()
  }
}