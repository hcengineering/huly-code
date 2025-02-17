// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers

import com.intellij.codeInsight.inline.completion.elements.InlineCompletionElement
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.flow.Flow

interface InlineCompletionProviderService {
  val name: String

  /** uses for switching between inline completion providers */
  fun start()
  /** uses for switching between inline completion providers */
  fun stop()

  /** displays the status of the provider in status bar */
  fun getStatus(): String
  /** popup actions of the provider in status bar */
  fun getActions(file: VirtualFile?): List<AnAction>

  fun documentOpened(file: VirtualFile, content: String) {}
  fun documentClosed(file: VirtualFile) {}
  fun documentChanged(file: VirtualFile, event: DocumentEvent) {}
  fun completionAccepted() {}
  fun completionRejected() {}
  fun update(file: VirtualFile, content: String, cursorOffset: Int) {}
  suspend fun suggest(file: VirtualFile, document: Document, cursorOffset: Int, tabSize: Int, insertTabs: Boolean): Flow<InlineCompletionElement>?
}