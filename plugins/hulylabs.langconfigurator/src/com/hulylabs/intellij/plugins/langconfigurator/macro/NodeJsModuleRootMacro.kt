// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.macro

import com.intellij.ide.macro.Macro
import com.intellij.ide.macro.PathMacro
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.vfs.isFile
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls

class NodeJsModuleRootMacro : Macro(), PathMacro {
  override fun getName(): @NonNls String = "NodeJsModuleRoot"

  override fun getDescription(): @Nls(capitalization = Nls.Capitalization.Sentence) String = "Path to the root of the NodeJS module for the current file"

  override fun expand(dataContext: DataContext): String? {
    var file = CommonDataKeys.VIRTUAL_FILE.getData(dataContext)
    var project = CommonDataKeys.PROJECT.getData(dataContext)
    if (project != null) {
      val root = project.baseDir
      file?.let {
        if (file.isFile) {
          file = file.parent
        }

        while (file != null && file != root && file.findChild("package.json") == null) {
          file = file.parent
        }
        return file?.path
      }
    }
    return null
  }
}