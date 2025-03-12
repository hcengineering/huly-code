// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.gitignore

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

private val LOG = Logger.getInstance(GitIgnoreSyncService::class.java)

@Service(Service.Level.PROJECT)
class GitIgnoreSyncService(val project: Project) {
  var contentEntryGitIgnores = mutableMapOf<VirtualFileUrl, GitIgnoreFile>()

  fun syncState() {
    LOG.info("Syncing .gitignore file with project exclude folders") // TODO: add support for multiple .gitignore files
    val virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
    for (module in project.modules) {
      val newContentEntryGitIgnores = mutableMapOf<VirtualFileUrl, GitIgnoreFile>()
      for (contentEntry in module.rootManager.contentEntries) {
        val root = contentEntry.file
        if (root == null || !root.isDirectory) {
          continue
        }
        val gitIgnoreFile = root.findChild(".gitignore") ?: continue
        val rootUrl = root.toVirtualFileUrl(virtualFileUrlManager)
        if (contentEntryGitIgnores.containsKey(rootUrl)) {
          val oldEntry = contentEntryGitIgnores[rootUrl]!!
          if (gitIgnoreFile.timeStamp == oldEntry.modificationStamp) {
            continue
          }
        }
        val directoryIgnore = GitIgnoreFile.parse(gitIgnoreFile)
        newContentEntryGitIgnores[rootUrl] = directoryIgnore
      }
      contentEntryGitIgnores = newContentEntryGitIgnores
      runInEdt {
        ModuleRootModificationUtil.updateModel(module) { model ->
          for (contentEntry in model.contentEntries) {
            val baseDir = contentEntry.file ?: continue
            for ((dir, patterns) in contentEntryGitIgnores) {
              if (dir.url == baseDir.url) {
                for (path in patterns.paths) {
                  contentEntry.addExcludeFolder(dir.url + if (path.startsWith("/")) path else "/$path")
                }
                contentEntry.excludePatterns = patterns.simplePatterns
              }
            }
          }
        }
      }
    }
  }
}
