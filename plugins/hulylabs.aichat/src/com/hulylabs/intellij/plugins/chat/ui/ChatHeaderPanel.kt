// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.ui

import com.hulylabs.intellij.plugins.chat.api.ChatMessage
import com.hulylabs.intellij.plugins.chat.settings.ChatHistory
import com.hulylabs.intellij.plugins.chat.settings.ChatSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class RegenerateAction(
  private val onAction: suspend () -> Unit,
) : AnAction("Regenerate Title", "", AllIcons.Actions.Refresh) {
  private val progressIcon: Icon = AnimatedIcon.FS()
  private var processing = false

  init {
    templatePresentation.disabledIcon = progressIcon
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  @OptIn(DelicateCoroutinesApi::class)
  override fun actionPerformed(e: AnActionEvent) {
    processing = true
    GlobalScope.launch {
      onAction()
      processing = false
      update(e)
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = !processing
  }
}

class TokenCountAction() : AnAction("Token Count", "", null) {
  var tokenCountString: String = "0/0k"

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  init {
    templatePresentation.text = tokenCountString
    templatePresentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.text = tokenCountString
  }

  override fun actionPerformed(e: AnActionEvent) {}
}

class ChatHeaderPanel : JPanel(BorderLayout()) {
  private var isSuppressTitleFieldNotification = false

  private val titleField = JBTextField().apply {
    border = JBUI.Borders.empty(2, 4)
    isOpaque = false
  }

  private val tokenCountLabel = TokenCountAction()

  private val toolbar = ActionManager.getInstance().createActionToolbar(
    "ChatHeaderToolbar",
    DefaultActionGroup().apply {
      add(RegenerateAction { summarize() })
      addSeparator()
      add(tokenCountLabel)
    },
    true
  ).apply {
    layoutStrategy = ToolbarLayoutStrategy.NOWRAP_STRATEGY
    targetComponent = this@ChatHeaderPanel
  }

  init {
    border = JBUI.Borders.empty(4)

    add(JPanel(BorderLayout()).apply {
      isOpaque = false
      add(titleField, BorderLayout.CENTER)
      add(toolbar.component, BorderLayout.EAST)
    }, BorderLayout.CENTER)

    background = JBUI.CurrentTheme.CustomFrameDecorations.paneBackground()
    border = JBUI.Borders.compound(
      JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground(), 0, 0, 1, 0),
      JBUI.Borders.empty(4)
    )

    titleField.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        if (!isSuppressTitleFieldNotification) {
          ChatHistory.getInstance().updateConversationTitle(titleField.text.trim())
        }
      }
    })
  }

  fun setTitle(title: String) {
    isSuppressTitleFieldNotification = true
    titleField.text = title
    titleField.transferFocus()
    isSuppressTitleFieldNotification = false
  }

  fun getTitle(): String = titleField.text

  fun setTokenCount(used: Int, total: Int) {
    val totalStr = if (total > 1000) "${total / 1000}k" else "$total"
    val usedStr = if (used > 1000) "${used / 1000}k" else "$used"
    tokenCountLabel.tokenCountString = "$usedStr/$totalStr"
  }

  suspend fun summarize() {
    val model = ChatSettings.getInstance().activeLanguageModel ?: return
    val messages = ChatHistory.getInstance().loadConversationMessages(ChatHistory.getInstance().currentConversationId).toMutableList()
    if (messages.isEmpty()) {
      return
    }
    if (messages.size > 1 && messages.last().role == "user") {
      messages.removeLast()
    }
    messages += ChatMessage("Generate a concise 3-7 word title for this conversation, omitting punctuation. Go straight to the title, without any preamble and prefix like `Here's a concise suggestion:...` or `Title:`", "user")
    try {
      var title = ""
      model.provider.sendChatRequest(model, messages).collect {
        title += it.content
      }
      title = title.replace(Regex("""<think>[^<]*</think>""", RegexOption.MULTILINE), "")
      title = title.trim(' ', '-', ':', '.', ',', ';', '!', '?', '"', '\'', '(', ')', '[', ']', '{', '}', '<', '>', '|', '*', '\n', '\r', '\t')
      titleField.text = title.take(150)
    }
    catch (e: Exception) {
      Logger.getInstance(this.javaClass).error(e)
    }
  }
}