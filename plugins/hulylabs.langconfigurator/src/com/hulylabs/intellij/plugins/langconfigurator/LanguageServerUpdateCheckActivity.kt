// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator

import com.hulylabs.intellij.plugins.langconfigurator.templates.HulyLanguageServerTemplate
import com.hulylabs.intellij.plugins.langconfigurator.templates.HulyLanguageServerTemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.redhat.devtools.lsp4ij.LanguageServersRegistry
import com.redhat.devtools.lsp4ij.server.definition.launching.UserDefinedLanguageServerDefinition

fun UserDefinedLanguageServerDefinition.getTemplateIdAndVersion(): Pair<String, Long>? {
  val templateId = this.templateId ?: return null
  if (!templateId.startsWith(LanguageServerTemplateInstaller.TEMPLATE_PREFIX)) {
    return null
  }
  val templateIdWithoutPrefix = templateId.substring(LanguageServerTemplateInstaller.TEMPLATE_PREFIX.length)
  val versionIdx = templateIdWithoutPrefix.lastIndexOf("-v")
  if (versionIdx <= 0) {
    return null
  }
  val templateIdWithoutVersion = templateIdWithoutPrefix.substring(0, versionIdx)
  val versionStr = templateIdWithoutPrefix.substring(versionIdx + 2)
  val version = try {
    java.lang.Long.parseLong(versionStr, 10)
  }
  catch (e: NumberFormatException) {
    return null
  }
  return Pair(templateIdWithoutVersion, version)
}

class LanguageServerUpdateCheckActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    val templateManager = ApplicationManager.getApplication().getService(HulyLanguageServerTemplateManager::class.java)
    LanguageServersRegistry.getInstance().serverDefinitions.forEach { serverDefinition ->
      if (serverDefinition !is UserDefinedLanguageServerDefinition) {
        return@forEach
      }
      val template: HulyLanguageServerTemplate?
      var oldVersion: Long? = null
      val templateId = serverDefinition.templateId
      if (templateId != null) {
        val (templateIdWithoutVersion, version) = serverDefinition.getTemplateIdAndVersion() ?: return@forEach
        template = templateManager.getTemplateById(templateIdWithoutVersion)
        oldVersion = version
      }
      else {
        template = templateManager.findTemplateByName(serverDefinition.displayName)
      }
      if (template == null) {
        return@forEach
      }

      if (oldVersion == null || oldVersion < template.version) {
        project.getService(LanguageServerUpdateRegistry::class.java)
          .addUpdateNeeded(template.id, serverDefinition.id, oldVersion != null)
      }
    }
  }
}