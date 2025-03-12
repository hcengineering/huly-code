// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.gitignore

import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import com.intellij.workspaceModel.core.fileIndex.impl.PlatformInternalWorkspaceFileIndexContributor
import com.intellij.workspaceModel.ide.legacyBridge.findModule
import com.intellij.workspaceModel.ide.toPath

class ContentRootFileIndexIgnoreContributor : WorkspaceFileIndexContributor<ContentRootEntity>, PlatformInternalWorkspaceFileIndexContributor {
  override val entityClass: Class<ContentRootEntity>
    get() = ContentRootEntity::class.java

  override fun registerFileSets(entity: ContentRootEntity, registrar: WorkspaceFileSetRegistrar, storage: EntityStorage) {
    val module = entity.module.findModule(storage)
    if (module != null) {
      val gitIgnoreService = module.project.service<GitIgnoreSyncService>()
      gitIgnoreService.syncState()
      registrar.registerExclusionCondition(entity.url, fun(file: VirtualFile): Boolean {
        var isExcluded = false
        val directoryGitIgnore = gitIgnoreService.contentEntryGitIgnores.get(entity.url) ?: return false
        val entryPath = entity.url.toPath()
        val relativePath = entryPath.relativize(file.toNioPath()).toString()
        for ((pattern, regex) in directoryGitIgnore.regexs) {
          if (isExcluded != pattern.negated) {
            continue
          }
          if (regex.matches(relativePath)) {
            isExcluded = !pattern.negated
          }
        }
        return isExcluded
      }, entity)
    }
  }
}
