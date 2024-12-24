// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.gitignore

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText

private val LOG = Logger.getInstance(GitIgnoreSyncService::class.java)

@Service(Service.Level.PROJECT)
class GitIgnoreSyncService(val project: Project) {
  private var modificationStamp = 0L
  var excludeFolders = HashSet<String>()
    private set
  var excludePatterns = HashSet<String>()
    private set

  private fun reloadGitIgnore(file: VirtualFile) {
    LOG.info("Reloading .gitignore file")
    modificationStamp = file.timeStamp
    file.readText().lines().forEach { line ->
      var line = line.trim()
      if (!line.startsWith("#") && !line.isBlank()) {
        if (!line.contains('/') && (line.contains('*') || line.contains('.'))) {
          excludePatterns.add(line)
        }
        else {
          // normalize some patterns
          if (line.startsWith("**/")) {
            line = line.substring(3)
          }
          if (line.startsWith("*/")) {
            line = line.substring(2)
          }
          if (line.endsWith("/**")) {
            line = line.substring(0, line.length - 3)
          }
          if (line.endsWith("/*")) {
            line = line.substring(0, line.length - 2)
          }
          if (line.endsWith("/")) {
            line = line.substring(0, line.length - 1)
          }
          if (line.contains("*")) {
            LOG.warn("Unsupported pattern '$line' contains '*' in the middle of the string")
          } else {
            excludeFolders.add(line)
          }
        }
      }
    }
  }

  fun syncState() {
    LOG.info("Syncing .gitignore file with project exclude folders")
    // TODO: add support for multiple .gitignore files
    val module = project.modules.firstOrNull()
    if (module != null) {
      val gitIgnoreFile = module.rootManager.contentRoots.first().findChild(".gitignore")
      if (gitIgnoreFile != null && gitIgnoreFile.timeStamp != modificationStamp) {
        reloadGitIgnore(gitIgnoreFile)
        for ((index, _) in ModuleRootManager.getInstance(module).contentEntries.withIndex()) {
          runInEdt {
            ModuleRootModificationUtil.updateModel(module) { model ->
              model.contentEntries[index].excludePatterns = excludePatterns.toList()
            }
          }
        }
      }
    }
  }
}