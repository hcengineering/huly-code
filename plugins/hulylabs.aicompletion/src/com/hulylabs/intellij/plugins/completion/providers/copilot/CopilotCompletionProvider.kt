// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.copilot

import com.hulylabs.intellij.plugins.completion.providers.CompletionUtils
import com.hulylabs.intellij.plugins.completion.providers.InlineCompletionProviderService
import com.hulylabs.intellij.plugins.completion.providers.copilot.actions.LogoutAction
import com.hulylabs.intellij.plugins.completion.providers.copilot.actions.SignInAction
import com.hulylabs.intellij.plugins.completion.providers.copilot.lsp.AuthStatusKind
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionElement
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

private val LOG = Logger.getInstance("#copilot.completion-provider")

class CopilotCompletionProvider(project: Project) : InlineCompletionProviderService {
  private val copilot: CopilotService = CopilotService.getInstance(project)

  override val name: String
    get() = "Copilot"

  override fun start() {
    copilot.start()
  }

  override fun stop() {
    copilot.stop()
  }

  override fun getStatus(): String {
    when (copilot.getAgentStatus()) {
      AgentStatus.Starting -> return "Starting"
      AgentStatus.Started -> {
        when (copilot.getAuthStatus()) {
          AuthStatusKind.OK,
          AuthStatusKind.MaybeOk,
            -> return "Active"
          AuthStatusKind.NotSignedIn -> return "Not signed in"
          AuthStatusKind.NotAuthorized -> return "Not authorized"
          AuthStatusKind.FailedToGetToken -> return "Failed to get token"
          AuthStatusKind.TokenInvalid -> return "Token invalid"
          null -> return "Agent not initialized"
        }
      }
      AgentStatus.SignInProcess -> return "Signing in..."
      AgentStatus.Stopped -> return "Stopped"
    }
  }

  override fun getActions(file: VirtualFile?): List<AnAction> {
    if (copilot.getAgentStatus() == AgentStatus.Started) {
      if (copilot.getAuthStatus() == AuthStatusKind.OK) {
        return listOf(LogoutAction(copilot))
      }
      else if (copilot.getAuthStatus() == AuthStatusKind.NotSignedIn) {
        return listOf(SignInAction(copilot))
      }
    }
    return listOf()
  }

  override fun documentOpened(file: VirtualFile, content: String) {
    copilot.documentOpened(file, content)
  }

  override fun documentClosed(file: VirtualFile) {
    copilot.documentClosed(file)
  }

  override fun documentChanged(file: VirtualFile, event: DocumentEvent) {
    copilot.documentChanged(file, event)
  }

  override fun completionAccepted() {
    copilot.completionAccepted()
  }

  override fun completionRejected() {
    copilot.completionRejected()
  }

  override suspend fun suggest(file: VirtualFile, document: Document, cursorOffset: Int, tabSize: Int, insertTabs: Boolean): Flow<InlineCompletionElement>? {
    var completionResult = copilot.completion(file, document, cursorOffset, tabSize, insertTabs)
    LOG.debug("completionResult: $completionResult")
    if (completionResult == null || completionResult.completions.isEmpty()) {
      return null
    }
    val completion = completionResult.completions.first()
    var txt = completion.displayText

    var lineEndOffset = document.getLineEndOffset(document.getLineNumber(cursorOffset))
    var lineSuffix = document.immutableCharSequence.subSequence(cursorOffset, lineEndOffset).toString()
    return CompletionUtils.splitCompletion(txt, lineSuffix).asFlow()
  }
}
