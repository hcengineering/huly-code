// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.onsave

import com.hulylabs.intellij.plugins.langconfigurator.LSPFileTypesRegistry
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

open class BaseOnSaveSettingsState : BaseState() {
  var isEnabled by property(false)
  var languageIds by stringSet()
  var allLanguageIdsSelected by property(true)

  fun isFileSupported(project: Project, file: VirtualFile): Boolean {
    val state = project.service<ExecuteOnSaveSettings>().state
    if (state.allLanguageIdsSelected) {
      return true
    }
    val fileType = file.fileType
    if (fileType is LanguageFileType) {
      if (state.languageIds.contains(fileType.language.id)) {
        return true
      }
      val languageId = LSPFileTypesRegistry.instance.getLanguageId(file)
      if (languageId != null && state.languageIds.contains(languageId)) {
        return true
      }
    }
    return false
  }
}
