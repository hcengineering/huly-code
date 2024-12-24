// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator

import com.hulylabs.intellij.plugins.langconfigurator.gitignore.GitIgnoreSyncService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ProjectStartupActivity() : ProjectActivity {
  override suspend fun execute(project: Project) {
    val service: GitIgnoreSyncService = project.service()
    service.syncState()
  }
}