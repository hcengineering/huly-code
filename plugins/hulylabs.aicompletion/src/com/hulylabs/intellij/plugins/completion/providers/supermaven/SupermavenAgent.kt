// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.hulylabs.intellij.plugins.completion.CompletionProviderStateChangedListener
import com.hulylabs.intellij.plugins.completion.providers.supermaven.messages.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

private val LOG = Logger.getInstance(SupermavenAgent::class.java)

data class SupermavenCompletionState(
  val entryId: Int,
  val prefixOffset: Int,
  var chunks: List<String> = listOf(),
  var dedent: String = "",
  var end: Boolean = false,
)

enum class SupermavenAccountStatus {
  UNKNOWN, NEEDS_ACTIVATION, READY;
}

private const val SM_MESSAGE_PREFIX = "SM-MESSAGE"

class SupermavenAgent(val project: Project, agentPath: Path) {
  private val process: Process

  private val stdout: BufferedReader
  private val stderr: BufferedReader
  private val stdin: BufferedWriter

  private val outputMessages = ArrayBlockingQueue<SupermavenOutboundMessage>(2048)
  private val incomingMessages = ArrayBlockingQueue<SupermavenMessage>(2048)

  private val json = Json {
    ignoreUnknownKeys = true
  }

  internal val states: TreeMap<Long, SupermavenCompletionState> = TreeMap()
  internal var accountStatus: SupermavenAccountStatus = SupermavenAccountStatus.UNKNOWN
  internal var serviceTier: String? = null
  internal var activationUrl: String? = null
  internal var newStateId = 1L

  init {
    val logPath = agentPath.parent.resolve("sm-agent.log")
    Files.deleteIfExists(logPath)
    val processBuilder = ProcessBuilder(agentPath.toString(), "stdio")
    processBuilder.environment()["SM_LOG_PATH"] = logPath.toString()
    process = processBuilder.start()
    stdout = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
    stderr = BufferedReader(InputStreamReader(process.errorStream, Charsets.UTF_8))
    stdin = process.outputWriter(Charsets.UTF_8)
    thread {
      while (true) {
        val msg = outputMessages.take()
        try {
          stdin.write(json.encodeToString(msg) + '\n')
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
        catch (_: IOException) {
          break
        }
        var line = str ?: continue
        if (!line.startsWith(SM_MESSAGE_PREFIX)) {
          LOG.warn("stdout: $line")
          continue
        } // remove prefix
        line = line.removePrefix(SM_MESSAGE_PREFIX)
        try {
          incomingMessages.put(json.decodeFromString<SupermavenMessage>(line))
        }
        catch (e: Exception) {
          LOG.error("error in process message (line=$line): ", e)
        }
      }
    }
    thread {
      while (true) {
        val str = try {
          stderr.readLine()
        }
        catch (_: IOException) {
          break
        }
        LOG.warn("stderr: ${str ?: continue}")
      }
    }

    // drain message thread
    thread {
      while (process.isAlive) {
        if (incomingMessages.isNotEmpty()) {
          ApplicationManager.getApplication()?.invokeLater {
            drainOutput()
          }
        }
        Thread.sleep(50)
      }
    }
    accountStatus = SupermavenAccountStatus.READY
  }

  fun drainOutput() {
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
              ResponseItemKind.TEXT -> chunks += item.text
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
          ApplicationManager.getApplication()?.invokeLater {
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
      is SupermavenMetadataMessage -> {
        // begin communication with agent
        ApplicationManager.getApplication()?.invokeLater {
          val settings = ApplicationManager.getApplication().service<SupermavenSettings>()
          send(SupermavenGreetingsMessage(settings.state.gitignoreAllowed))
          // send dummy update to start communication
          send(SupermavenStateUpdateMessage(newId = "0", updates = listOf(FileUpdateMessage(path = "/dummy", content = "dummy"),
                                                                          CursorPositionUpdateMessage(path = "/dummy", offset = 0))))
        }
      }
      is SupermavenSetMessage -> {
      }
      else -> {
        LOG.warn("unhandled message: $message")
      }
    }
    ApplicationManager.getApplication()?.invokeLater {
      project.messageBus.syncPublisher(CompletionProviderStateChangedListener.TOPIC).stateChanged()
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