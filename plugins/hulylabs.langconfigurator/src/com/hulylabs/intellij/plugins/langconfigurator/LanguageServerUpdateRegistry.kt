// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator

import com.hulylabs.intellij.plugins.langconfigurator.templates.HulyLanguageServerTemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServersRegistry
import com.redhat.devtools.lsp4ij.server.definition.launching.UserDefinedLanguageServerDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NotNull

@Service(Service.Level.PROJECT)
class LanguageServerUpdateRegistry(private val project: Project, private val coroutineScope: CoroutineScope) {
  companion object {
    private val LOG = Logger.getInstance(LanguageServerUpdateRegistry::class.java)
  }
  private val updateNeeded = mutableMapOf<String, Pair<String, Boolean>>()
  fun addUpdateNeeded(@NotNull templateId: String, serverId: String, silent: Boolean) {
    synchronized(updateNeeded) {
      updateNeeded[templateId] = Pair(serverId, silent)
    }
    if (silent) {
      coroutineScope.launch {
        val serverDefinition = LanguageServersRegistry.getInstance().serverDefinitions.find { it.id == serverId } as? UserDefinedLanguageServerDefinition
                               ?: return@launch
        val templateManager = ApplicationManager.getApplication().getService(HulyLanguageServerTemplateManager::class.java)
        val template = templateManager.getTemplateById(templateId) ?: return@launch
        LanguageServerTemplateInstaller.update(project, serverDefinition, template, { resetUpdateNeeded(templateId) }, {
          LOG.warn("Failed to update server '$serverId' with template '$templateId'")
        })
      }
      return
    }
  }

  fun resetUpdateNeeded(@NotNull templateId: String) {
    synchronized(updateNeeded) {
      updateNeeded.remove(templateId)
    }
  }

  fun getUpdateServerId(@NotNull templateId: String): Pair<String, Boolean>? {
    synchronized(updateNeeded) {
      return updateNeeded[templateId]
    }
  }
}