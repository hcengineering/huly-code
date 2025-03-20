// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.hulylabs.intellij.plugins.completion.CompletionSettings
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionReplaceElement
import com.hulylabs.intellij.plugins.completion.providers.InlineCompletionProviderService
import com.hulylabs.intellij.plugins.completion.providers.supermaven.actions.*
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionElement
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private val LOG = Logger.getInstance("#supermaven.completion-provider")

class SupermavenCompletionProvider(val project: Project) : InlineCompletionProviderService {
  private val MAX_LINES = 10
  private val supermaven: SupermavenService = SupermavenService.getInstance(project)
  override val name: String
    get() = "Supermaven"

  override fun start() {
    supermaven.start()
  }

  override fun stop() {
    supermaven.stop()
  }

  override fun getStatus(): String {
    return when (supermaven.state) {
      SupermavenService.AgentState.STARTING -> {
        when (supermaven.getAccountStatus()) {
          SupermavenAccountStatus.NEEDS_ACTIVATION -> "Needs activation"
          else -> "Starting"
        }
      }
      SupermavenService.AgentState.FAILED_DOWNLOAD -> "Failed to download agent"
      SupermavenService.AgentState.STARTED -> {
        when (supermaven.getAccountStatus()) {
          SupermavenAccountStatus.NEEDS_ACTIVATION -> "Needs activation"
          SupermavenAccountStatus.CONNECTING -> "Connecting"
          SupermavenAccountStatus.READY -> {
            val repo = supermaven.getRepoName()?.let { " [${it}]" } ?: ""
            "${supermaven.getServiceTier() ?: "Unknown"}$repo"
          }
          else -> "Unknown"
        }
      }
      SupermavenService.AgentState.ERROR -> "Error"
      SupermavenService.AgentState.STOPPED -> "Stopped"
    }
  }

  override fun getActions(file: VirtualFile?): List<AnAction> {
    val actions: MutableList<AnAction> = mutableListOf()
    if (supermaven.getAccountStatus() == SupermavenAccountStatus.NEEDS_ACTIVATION) {
      actions.add(FreeActivationAction(supermaven))
      actions.add(ProActivationAction(supermaven))
    }
    else if (supermaven.state == SupermavenService.AgentState.STARTED) {
      if (supermaven.getServiceTier() == "FreeNoLicense") {
        actions.add(UpgradeProAction(supermaven))
      }
      val settings = ApplicationManager.getApplication().service<SupermavenSettings>()
      actions.add(ToggleGitignoreAction(supermaven, settings.state.gitignoreAllowed))
      actions.add(LogoutAction(supermaven))
    }
    return actions
  }

  private fun isFileSupported(file: VirtualFile): Boolean {
    return file.extension != null && !CompletionSettings.getInstance().state.disabledExtensions.contains(file.extension)
  }

  override fun update(file: VirtualFile, content: String, cursorOffset: Int) {
    if (!isFileSupported(file)) {
      return
    }
    supermaven.update(file.path, content, cursorOffset)
  }

  private fun limitLines(text: String): String {
    val lines = text.split("\n")
    return if (lines.size > MAX_LINES) {
      lines.subList(0, MAX_LINES).joinToString("\n")
    }
    else {
      text
    }
  }

  private fun lineSpans(str: String): MutableList<Pair<Int, Int>> {
    var spans = mutableListOf<Pair<Int, Int>>()
    var start = 0
    var i = 0
    if (str.isNotEmpty()) {
      while (true) {
        if (i == str.length || str[i] == '\n') {
          if (str.substring(start, i).trim().isNotEmpty()) {
            spans.add(Pair(start, i))
          }
          start = i + 1
        }
        if (i == str.length) {
          break
        }
        i++
      }
    }

    return spans
  }

  private fun trimMatchingSuffixWithLinePrefix(linePrefix: String, completion: String, suffix: String): String {
    val trimmed = trimMatchingSuffix(linePrefix + completion, suffix)
    var str: String
    if (trimmed.length >= linePrefix.length) {
      str = trimmed.substring(linePrefix.length)
    }
    else {
      str = ""
    }

    return if (str.trim().isEmpty()) this.trimToFirstNewline(completion) else str
  }

  private fun trimToFirstNewline(str: String): String {
    val idx = str.indexOf('\n')
    if (idx == -1) {
      return str
    }
    else {
      return str.substring(0, idx)
    }
  }

  private fun trimMatchingSuffix(completion: String, suffix: String): String {
    val completionSpans = lineSpans(completion)
    val suffixSpans = lineSpans(suffix)
    var completionLines = mutableListOf<String>()
    var suffixLines = mutableListOf<String>()

    for (span in completionSpans) {
      completionLines.add(completion.substring(span.first, span.second))
    }

    for (span in suffixSpans) {
      suffixLines.add(suffix.substring(span.first, span.second))
    }

    var idx = this.trimMatchingSuffixLines(completionLines, suffixLines)
    return if (idx == 0) "" else completion.substring(0, completionSpans[idx - 1].second)
  }

  private fun trimMatchingSuffixLines(completion: List<String>, suffix: List<String>): Int {
    var i = 0
    while (i < completion.size) {
      if (startsWith(suffix, completion.slice(i until completion.size))) {
        return i
      }
      i++
    }
    return completion.size
  }

  private fun startsWith(body: List<String>, candidate: List<String>): Boolean {
    var i = 0
    while (i < candidate.size) {
      if (i >= body.size || body[i] != candidate[i]) {
        return false
      }
      i++
    }
    return true
  }

  override suspend fun suggest(file: VirtualFile, document: Document, cursor: Int, tabSize: Int, insertTabs: Boolean): Flow<InlineCompletionElement>? {
    LOG.debug("suggest")
    if (!isFileSupported(file)) {
      LOG.debug("isFileSupported ${file.extension} false")
      return null
    }
    val path = file.path
    val content = document.text
    return flow {
      delay(100)
      var lineStartIdx = content.lastIndexOf('\n', cursor - 1)
      var lineEndIdx = content.indexOf('\n', cursor)
      if (lineStartIdx == -1) {
        lineStartIdx = 0
      }
      if (lineEndIdx == -1) {
        lineEndIdx = content.length
      }
      val params = CompletionParams(content.substring(lineStartIdx, cursor), content.substring(cursor, lineEndIdx))
      val completion = supermaven.completion(path, content, cursor, params) ?: return@flow
      LOG.trace("found completion ${completion}")
      if (!params.lineBeforeCursor.endsWith(completion.dedent)) {
        return@flow
      }
      var dedent = completion.dedent
      var text = limitLines(completion.text)
      val suffix = content.substring(cursor + params.lineAfterCursor.length)
      text = trimMatchingSuffixWithLinePrefix(params.lineBeforeCursor, text, suffix)
      text = text.trimEnd()
      var needReplace = false

      while (dedent.isNotEmpty() && text.isNotEmpty() && dedent[0] == text[0]) {
        dedent = dedent.substring(1)
        text = text.substring(1)
      }

      // trim prefix
      var prefix = params.lineBeforeCursor
      while (prefix.isNotEmpty()) {
        if (text.startsWith(prefix)) {
          text = text.substring(prefix.length)
          break
        }
        prefix = prefix.substring(1)
      }

      if (text.endsWith(params.lineAfterCursor)) {
        text = text.substring(0, text.length - params.lineAfterCursor.length)
        text = text.trimEnd()
      }
      else {
        needReplace = true
      }

      if (text.isNotEmpty()) {
        if (needReplace) {
          val lineBeforeCursor = params.lineBeforeCursor.substring(0, params.lineBeforeCursor.length - dedent.length)
          val replaceText = lineBeforeCursor + text
          val startOffset = cursor - params.lineBeforeCursor.length
          val endOffset = cursor + params.lineAfterCursor.length
          emit(InlineCompletionReplaceElement(replaceText.trim(), replaceText, startOffset, endOffset, params.lineAfterCursor.length))
        }
        else {
          emit(InlineCompletionGrayTextElement(text))
        }
      }
    }
  }

  override fun documentChanged(file: VirtualFile, event: DocumentEvent) {
    supermaven.documentChanged(file.path)
  }
}