// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.copilot

import com.hulylabs.intellij.plugins.completion.CompletionProviderStateChangedListener
import com.hulylabs.intellij.plugins.completion.providers.copilot.lsp.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.future.await
import org.eclipse.lsp4j.jsonrpc.Launcher
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.concurrent.Future
import java.util.logging.LogRecord
import kotlin.concurrent.thread


private val LOG = Logger.getInstance("#copilot")

@Suppress("unused")
class LogMessageHandler : java.util.logging.Handler() {
  override fun publish(record: LogRecord) {
    println(record.message)
  }

  fun registerTo(name: String?): java.util.logging.Logger {
    val logger = java.util.logging.Logger.getLogger(name)
    logger.setUseParentHandlers(false)
    logger.addHandler(this)
    logger.setLevel(java.util.logging.Level.ALL)
    return logger
  }

  override fun flush() {
  }

  override fun close() {
  }
}

class CopilotAgent(
  private val project: Project,
  private val nodeBinaryPath: Path,
  private val agentPath: Path,
) {

  private lateinit var stderr: BufferedReader
  private lateinit var process: Process
  private lateinit var launcherResult: Future<Void>

  lateinit var server: CopilotLSPServer
    private set

  var authStatus: AuthStatusKind = AuthStatusKind.NotSignedIn

  suspend fun start() {
    val processBuilder = ProcessBuilder(nodeBinaryPath.toString(), agentPath.toString(), "--stdio")
    processBuilder.environment()["PATH"] = nodeBinaryPath.parent.toString().replace('\\', '/')
    processBuilder.directory(agentPath.parent.toFile())
    process = processBuilder.start()

    var builder = Launcher.Builder<CopilotLSPServer>()
      .setLocalService(CopilotService.getInstance(project))
      .setRemoteInterface(CopilotLSPServer::class.java)
      .setInput(process.inputStream)
      .setOutput(process.outputStream)
      .validateMessages(false)
    //if (LOG.isTraceEnabled) {
    //  builder = builder.traceMessages(PrintWriter(System.out))
    //}
    val launcher = builder.create()
    launcherResult = launcher.startListening()
    //val h = LogMessageHandler()
    //h.registerTo(GenericEndpoint::class.java.name)
    server = launcher.remoteProxy
    val workspaceFolders = project.guessProjectDir()?.let { listOf(WorkspaceFolder(fromFile(it))) } ?: emptyList()
    val res = server.initialize(
      InitializeParams(process.pid(),
                       NameVersionInfo("Huly Code", "2025.1"),
                       NameVersionInfo("hulylabs.aicompletion", "0.1.0"),
                       workspaceFolders)).await()
    LOG.info("Server initialized: ${res?.capabilities}")
    server.initialized()
    authStatus = server.checkStatus().await()?.status
                 ?: AuthStatusKind.NotSignedIn
    stderr = BufferedReader(InputStreamReader(process.errorStream, Charsets.UTF_8))
    thread {
      while (true) {
        val str = try {
          stderr.readLine()
        }
        catch (_: IOException) {
          break
        }
        if (process.isAlive != true) {
          break
        }
        if (str == null) {
          Thread.sleep(10)
          continue
        }
        LOG.warn("stderr: ${str}")
      }
      LOG.info("CopilotAgent destroyed")
      project.messageBus.syncPublisher(CompletionProviderStateChangedListener.TOPIC).stateChanged()
    }
    LOG.info("CopilotAgent initialized")
  }

  fun destroy() {
    process.destroy()
  }

  fun isAlive(): Boolean {
    return process.isAlive == true && launcherResult.isDone == false
  }
}