// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileNameMatcher
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import com.redhat.devtools.lsp4ij.LanguageServersRegistry
import com.redhat.devtools.lsp4ij.server.definition.LanguageServerDefinitionListener

class LSPFileTypesRegistry private constructor() : LanguageServerDefinitionListener {
  private val languageIds: MutableSet<String> = mutableSetOf()
  private val languageMappings: MutableMap<Language, String> = mutableMapOf()
  private var fileTypeMappings: MutableMap<FileType, String> = mutableMapOf()
  private var filenameMatcherMappings: MutableMap<FileNameMatcher, String> = mutableMapOf()

  override fun handleAdded(event: LanguageServerDefinitionListener.LanguageServerAddedEvent) {
    refresh()
  }

  override fun handleRemoved(event: LanguageServerDefinitionListener.LanguageServerRemovedEvent) {
    refresh()
  }

  override fun handleChanged(event: LanguageServerDefinitionListener.LanguageServerChangedEvent) {
    refresh()
  }

  companion object {
    val instance: LSPFileTypesRegistry by lazy {
      LSPFileTypesRegistry()
    }
  }

  init {
    refresh()
    LanguageServersRegistry.getInstance().addLanguageServerDefinitionListener(this)
  }

  private fun refresh() {
    languageIds.clear()
    languageMappings.clear()
    LanguageServersRegistry.getInstance().serverDefinitions.forEach { definition ->
      definition.languageMappings.forEach { (language, languageId) ->
        languageIds.add(languageId)
        languageMappings[language] = languageId
      }
      definition.fileTypeMappings.forEach { (fileType, languageId) ->
        languageIds.add(languageId)
        fileTypeMappings[fileType] = languageId
      }
      definition.filenameMatcherMappings.forEach { pair ->
        val languageId = pair.second
        val patterns = pair.first
        languageIds.add(languageId)
        for (pattern in patterns) {
          filenameMatcherMappings[pattern] = languageId
        }
      }
    }
  }

  fun getLanguageIds(): Set<String> {
    return languageIds
  }

  fun getLanguageId(file: VirtualFile): String? {
    if (file.fileType is LanguageFileType) {
      val language = (file.fileType as LanguageFileType).language
      if (languageMappings.contains(language)) {
        return languageMappings[language]
      }
    }
    val fileType = FileTypeManager.getInstance().getFileTypeByFileName(file.name)
    if (fileTypeMappings.contains(fileType)) {
      return fileTypeMappings[fileType]
    }
    filenameMatcherMappings.forEach { (matcher, languageId) ->
      if (matcher.acceptsCharSequence(file.path)) {
        return languageId
      }
    }
    return null
  }
}