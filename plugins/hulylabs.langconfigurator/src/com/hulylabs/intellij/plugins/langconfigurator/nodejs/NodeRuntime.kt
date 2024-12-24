// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.nodejs

import java.io.IOException
import java.nio.file.Path

abstract class NodeRuntime {

  companion object {
    @JvmStatic
    suspend fun instance(): NodeRuntime {
      return ManagedNodeRuntime.installIfNeeded()
    }
  }

  protected abstract fun binaryPath(): Path

  @Throws(IOException::class)
  protected abstract suspend fun runNpmSubcommand(directory: Path?, subcommand: String, vararg args: String): String?

  @Throws(IOException::class)
  suspend fun npmInstallPackages(directory: Path?, vararg packages: String) {
    if (packages.isEmpty()) {
      return
    }

    runNpmSubcommand(directory,
                     "install",
                     *packages,
                     "--prefix", ".",
                     "--save-exact",
                     "--fetch-retry-mintimeout", "2000",
                     "--fetch-retry-maxtimeout", "5000",
                     "--fetch-timeout", "5000")
  }
}