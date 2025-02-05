// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.IconManager.Companion.getInstance
import javax.swing.Icon

class CustomFileIconProvider : FileIconProvider {

  override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
    if (file.fileType.name == "TOML") {
      return getInstance().getIcon("icons/toml.svg", CustomFileIconProvider::class.java.classLoader)
    }
    val ext = file.extension
    when (ext) {
      "rs" -> return getInstance().getIcon("icons/rust.svg", CustomFileIconProvider::class.java.classLoader)
      "ts", "jsx", "tsx" -> return getInstance().getIcon("icons/typescript.svg", CustomFileIconProvider::class.java.classLoader)
      "md", "mdx" -> return getInstance().getIcon("icons/readme.svg", CustomFileIconProvider::class.java.classLoader)
      "toml" -> return getInstance().getIcon("icons/toml.svg", CustomFileIconProvider::class.java.classLoader)
      else -> return null
    }
  }

}