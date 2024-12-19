// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.utils

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.platform.util.progress.reportRawProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path

object ExecuteUtils {
  private val LOG = Logger.getInstance(ExecuteUtils::class.java)

  @JvmStatic
  @Throws(IOException::class)
  suspend fun execCommand(
    title: String,
    baseCommandLine: GeneralCommandLine,
    workingDir: Path? = null,
    returnStdout: Boolean = false,
  ): String? {
    var commandLine = baseCommandLine.withCharset(StandardCharsets.UTF_8).withRedirectErrorStream(true)
    if (workingDir != null) {
      commandLine = commandLine.withWorkingDirectory(workingDir)
    }
    LOG.info("Execute command: " + commandLine.commandLineString)
    val processHandler: ProcessHandler
    try {
      processHandler = OSProcessHandler(commandLine)
    }
    catch (e: ExecutionException) {
      throw IOException("Cannot execute command $commandLine", e)
    }

    val runner = CapturingProcessRunner(processHandler)
    return reportRawProgress { reporter ->
      withContext(Dispatchers.IO) {
        reporter.text(title);
        processHandler.addProcessListener(object : ProcessAdapter() {
          override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
            LOG.info(event.text);
            reporter.details(event.text);
          }
        })
        val output = runner.runProcess(100_000, true)
        if (output.exitCode != 0) {
          LOG.warn("Command failed with exit code: " + output.exitCode)
          throw IOException("Command failed with exit code: " + output.exitCode)
        }
        if (returnStdout) output.stdout
        else null
      }
    }
  }

  @JvmStatic
  @Throws(IOException::class)
  suspend fun execCommand(
    title: String,
    executable: String,
    workingDir: Path? = null,
    returnStdout: Boolean = false,
    vararg args: @NotNull String,
  ): String? {
    val exePath = PathEnvironmentVariableUtil.findExecutableInWindowsPath(executable)
    var commandLine =
      GeneralCommandLine()
        .withExePath(exePath)
        .withParameters(*args)
    return execCommand(title, commandLine, workingDir, returnStdout)
  }
}