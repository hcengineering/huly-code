// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.intellij.build.io.runProcess
import java.nio.file.Path

class MacOsSignTool : SignTool {
  override val signNativeFileMode: SignNativeFileMode
  get() = SignNativeFileMode.ENABLED

  override suspend fun signFiles(files: List<Path>, context: BuildContext?, options: PersistentMap<String, String>) {
    if (context == null || !context.options.targetOs.contains(OsFamily.MACOS)) {
      return
    }

    for (file in files) {
      println("Sign file $file")
      var args = ArrayList<String>()
      args += "codesign"
      args += "--verbose"
      args += "--sign"
      args += "Developer ID Application"
      val codesignOptions = options["mac_codesign_options"]
      if (codesignOptions != null) {
        args += "--options"
        args += codesignOptions
      }
      val codesignForce = options["mac_codesign_force"]?.toBoolean() == true
      if (codesignForce) {
        args += "--force"
      }
      val codesignEntitlements = options["mac_codesign_entitlements"]
      if (codesignEntitlements != null) {
        args += "--entitlements"
        args += codesignEntitlements
      }
      args += file.toString()
      //val contentType = options["mac_codesign_content_type"]
      withContext(Dispatchers.IO) {
        runBlocking {
          runProcess(args, workingDir = context?.paths?.artifactDir, inheritOut = true)
        }
      }
    }
  }

  override suspend fun signFilesWithGpg(files: List<Path>, context: BuildContext) {
    signFiles(files, context, persistentMapOf())
  }

  override suspend fun getPresignedLibraryFile(path: String, libName: String, libVersion: String, context: BuildContext): Path? {
    println("getPresignedLibraryFile $path, $libName, $libVersion")
    return null
  }

  override suspend fun commandLineClient(context: BuildContext, os: OsFamily, arch: JvmArchitecture): Path? {
    return null
  }
}