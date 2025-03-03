// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.utils

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import java.nio.file.Path

object NodeUtils {
  fun findNodeModuleRootDir(file: VirtualFile): Path? {
    var dir = file.parent
    while (dir != null) {
      for (child in dir.children) {
        if (child.isFile && child.name == "package.json") {
          return dir.toNioPath()
        }
      }
      dir = dir.parent
    }
    return null
  }
}