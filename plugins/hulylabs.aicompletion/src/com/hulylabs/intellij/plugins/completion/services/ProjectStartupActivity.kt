// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.services

import com.hulylabs.intellij.plugins.completion.InlineCompletionProviderRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ProjectStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    InlineCompletionProviderRegistry.getInstance(project)
  }
}