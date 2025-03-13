// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.ui

import com.hulylabs.intellij.plugins.chat.settings.ChatHistory
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent

class ChatHeaderPanel : JPanel(BorderLayout()) {
  private var isSuppressTitleFieldNotification = false

  private val titleField = JBTextField().apply {
    border = JBUI.Borders.empty(2, 4)
    isOpaque = false
  }

  private val tokenCountLabel = JBLabel("0/0k").apply {
    border = JBUI.Borders.empty(2, 10)
    preferredSize = Dimension(100, titleField.preferredSize.height)
    horizontalAlignment = SwingConstants.RIGHT
    foreground = UIUtil.getContextHelpForeground()
  }

  init {
    border = JBUI.Borders.empty(4)

    // Left side - title
    add(titleField, BorderLayout.CENTER)

    // Right side - token count
    add(JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
      isOpaque = false
      add(tokenCountLabel)
    }, BorderLayout.EAST)

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
    tokenCountLabel.text = "$used/$totalStr"
  }
}