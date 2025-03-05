// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator

import com.hulylabs.intellij.plugins.langconfigurator.gitignore.GitIgnoreSyncService
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.registry.EarlyAccessRegistryManager

class ProjectStartupActivity() : ProjectActivity {
  override suspend fun execute(project: Project) {
    val service: GitIgnoreSyncService = project.service()
    service.syncState()
    PropertiesComponent.getInstance().setValue("ide.try.ultimate.disabled", true)
    EarlyAccessRegistryManager.setString("idea.plugins.compatible.build", "243.23654")
  }
}