// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.gitignore

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class GitIgnoreFileListener : BulkFileListener {
  override fun after(events: MutableList<out VFileEvent>) {
    if (events.isEmpty()) return
    // TODO: add support for multiple .gitignore files
    val hasGitIgnoreFile = events.any { event -> event.path.endsWith(".gitignore")}
    if (hasGitIgnoreFile) {
      ProjectUtil.getActiveProject()?.let { project ->
        val service: GitIgnoreSyncService = project.service()
        service.syncState()
      }
    }
  }
}