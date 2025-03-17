// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion

import com.intellij.codeInsight.inline.completion.*
import com.intellij.codeInsight.inline.completion.session.InlineCompletionSession
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionSuggestion
import com.intellij.codeInsight.inline.completion.suggestion.InlineCompletionVariant
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JComponent
import javax.swing.JLabel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val LOG = Logger.getInstance("#hulycode-inline")

class EditorInlineCompletionProvider : DebouncedInlineCompletionProvider(), LookupManagerListener {
  var lookupListenerAdded = false
  var lookupShown = false

  override val id: InlineCompletionProviderID
    get() = InlineCompletionProviderID("AIInlineCompletionProvider")

  override val providerPresentation: InlineCompletionProviderPresentation
    get() = object : InlineCompletionProviderPresentation {
      override fun getTooltip(project: Project?): JComponent {
        return JLabel(if (project != null) InlineCompletionProviderRegistry.getProvider(project).name else "Inline Completion")
      }
    }

  override suspend fun getDebounceDelay(request: InlineCompletionRequest): Duration {
    return 1.seconds
  }

  override suspend fun getSuggestionDebounced(request: InlineCompletionRequest): InlineCompletionSuggestion {
    LOG.trace("getSuggestion")
    if (!ApplicationManager.getApplication().service<CompletionSettings>().isCompletionEnabled(request.editor.virtualFile)) {
      return InlineCompletionSuggestion.Empty
    }
    if (!lookupListenerAdded && request.editor.project != null) {
      request.editor.project!!.messageBus.connect().subscribe(LookupManagerListener.TOPIC, this)
      lookupListenerAdded = true
    }
    if (lookupShown) {
      LOG.trace("hide suggestion because lookup is shown")
      InlineCompletionSuggestion.Empty
    }
    return object : InlineCompletionSuggestion {
      override suspend fun getVariants(): List<InlineCompletionVariant> {
        val editor = request.editor
        val provider = InlineCompletionProviderRegistry.getProvider(editor.project!!)
        val caretOffset = withContext(Dispatchers.EDT) {
          editor.caretModel.offset
        }
        val tabSize = editor.settings.getTabSize(editor.project)
        val insertTabs = editor.settings.isUseTabCharacter(editor.project)
        var data = UserDataHolderBase()
        val elements = provider.suggest(editor.virtualFile, editor.document, caretOffset, tabSize, insertTabs)
        if (elements != null) {
          return listOf(InlineCompletionVariant.build(data, elements))
        }
        else {
          return emptyList()
        }
      }
    }
  }

  override fun restartOn(event: InlineCompletionEvent): Boolean {
    return event is InlineCompletionEvent.LookupCancelled
  }

  override fun isEnabled(event: InlineCompletionEvent): Boolean {
    var settings = CompletionSettings.getInstance()
    if (!settings.isCompletionEnabled()) {
      return false
    }
    if (lookupShown) {
      return false
    }
    if (event !is InlineCompletionEvent.DirectCall && settings.state.onlyDirectCalls) {
      return false
    }
    return event is InlineCompletionEvent.DocumentChange
           || event is InlineCompletionEvent.LookupCancelled
           || event is InlineCompletionEvent.DirectCall
           || event is InlineCompletionEvent.SuggestionInserted
  }

  override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
    lookupShown = false
    newLookup?.addLookupListener(object : LookupListener {
      override fun lookupCanceled(event: LookupEvent) {
        lookupShown = false
      }

      override fun lookupShown(event: LookupEvent) {
        lookupShown = true
        val handler = InlineCompletion.getHandlerOrNull(event.lookup.editor)
        val currentSession = InlineCompletionSession.getOrNull(event.lookup.editor)
        if (currentSession != null) {
          handler?.invokeEvent(InlineCompletionEvent.LookupCancelled(event.lookup.editor, event))
        }
      }
    })
  }
}