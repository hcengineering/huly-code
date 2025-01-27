// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.vfs.VirtualFile

interface InlineCompletionProviderService {
  val name: String

  /** uses for switching between inline completion providers */
  fun start()
  /** uses for switching between inline completion providers */
  fun stop()

  fun getStatus(): String
  fun getActions(file: VirtualFile?): List<AnAction>
  fun update(file: VirtualFile, content: String, entryId: Int, cursorOffset: Int)
  fun suggest(file: VirtualFile, content: String, entryId: Int, cursorOffset: Int): String?
}