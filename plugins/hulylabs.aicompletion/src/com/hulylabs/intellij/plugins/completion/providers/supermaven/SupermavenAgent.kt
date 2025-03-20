// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.hulylabs.intellij.plugins.completion.CompletionProviderStateChangedListener
import com.hulylabs.intellij.plugins.completion.providers.supermaven.messages.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
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

private val LOG = Logger.getInstance("#supermaven.agent")

enum class SupermavenAccountStatus {
  UNKNOWN, NEEDS_ACTIVATION, CONNECTING, READY;
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
    coerceInputValues = true
  }

  internal val states: TreeMap<Long, SupermavenCompletionState> = TreeMap()
  internal var accountStatus: SupermavenAccountStatus = SupermavenAccountStatus.UNKNOWN
  internal var dustStrings: List<String> = emptyList()
  internal var serviceTier: String? = null
  internal var repoName: String? = null
  internal var activationUrl: String? = null
  internal var newStateId = 1L

  init {
    val processBuilder = ProcessBuilder(agentPath.toString(), "stdio")
    if (LOG.isDebugEnabled) {
      val logPath = PathManager.getLogDir().resolve("sm-agent-${System.currentTimeMillis()}.log")
      try {
        Files.deleteIfExists(logPath)
      }
      catch (_: Exception) {
        // ignore exception
      }
      processBuilder.environment()["SM_LOG_PATH"] = logPath.toString()
    }
    process = processBuilder.start()
    stdout = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
    stderr = BufferedReader(InputStreamReader(process.errorStream, Charsets.UTF_8))
    stdin = process.outputWriter(Charsets.UTF_8)
    thread {
      while (true) {
        val msg = outputMessages.take()
        try {
          LOG.trace("sending message: ${json.encodeToString(msg)}")
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
        if (str == null) {
          Thread.sleep(100)
          continue
        }
        var line = str
        LOG.trace("received message: $line")
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
        if (str == null) {
          Thread.sleep(100)
          continue
        }
        LOG.warn("stderr: ${str}")
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
    accountStatus = SupermavenAccountStatus.CONNECTING
    send(SupermavenGreetingsMessage(SupermavenSettings.getInstance().state.gitignoreAllowed))
    send(SupermavenStateUpdateMessage(newId = "0", updates = listOf(FileUpdateMessage(path = "/dummy", content = "dummy"),
                                                                    CursorPositionUpdateMessage(path = "/dummy", offset = 0))))
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
            completion += item
            if (item.kind == ResponseItemKind.END
                || item.kind == ResponseItemKind.BARRIER
                || item.kind == ResponseItemKind.FINISH_EDIT) {
              end = true
            }
          }
        }
      }
      is SupermavenConnectionStatus -> {
        accountStatus = if (message.isConnected) {
          SupermavenAccountStatus.READY
        }
        else {
          SupermavenAccountStatus.CONNECTING
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
      is SupermavenUserStatus -> {
        serviceTier = message.tier
        accountStatus = SupermavenAccountStatus.READY
      }
      is SupermavenActiveRepo -> {
        repoName = message.displayText
      }
      is SupermavenPassthroughMessage -> {
        handleMessage(message.passthrough)
      }
      is SupermavenMetadataMessage -> {
        dustStrings = message.dustStrings
      }
      is SupermavenSetMessage -> {
        LOG.info("set ${message.key} ${message.value}")
      }
      is SupermavenSetV2Message -> {
        LOG.info("setV2 ${message.key} ${message.value}")
      }
      else -> {
        LOG.warn("unhandled message: $message")
      }
    }
    ApplicationManager.getApplication()?.invokeLater {
      project.messageBus.syncPublisher(CompletionProviderStateChangedListener.TOPIC).stateChanged()
    }
  }

  fun newCompletionState(path: String, prefix: String) {
    newStateId++
    states[newStateId] = SupermavenCompletionState(path, prefix)
    if (states.size > 50) {
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