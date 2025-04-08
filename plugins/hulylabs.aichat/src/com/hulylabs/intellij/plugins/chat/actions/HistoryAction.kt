// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.actions

import com.hulylabs.intellij.plugins.chat.settings.ChatConversationInfo
import com.hulylabs.intellij.plugins.chat.settings.ChatHistory
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import kotlin.time.Duration.Companion.milliseconds

fun formatDuration(startTimeMillis: Long): String {
  val duration = (System.currentTimeMillis() - startTimeMillis).milliseconds
  return duration.toComponents { days, hours, minutes, seconds, _ ->
    when {
      days > 0 -> "${days} days"
      hours > 0 -> "${hours} hours"
      minutes > 0 -> "${minutes} minutes"
      else -> "${seconds} seconds"
    }
  }
}

class HistoryAction(
  private val onPerform: (conversationId: Long) -> Unit,
) : AnAction("History", null, IconLoader.getIcon("/icons/history.svg", HistoryAction::class.java)) {

  override fun actionPerformed(e: AnActionEvent) {
    val history = ChatHistory.getInstance().history()

    val listModel = DefaultListModel<ChatConversationInfo>()
    history.forEach { listModel.addElement(it) }

    val list = JBList(listModel).apply {
      cellRenderer = HistoryItemRenderer { conversation ->
        listModel.removeElement(conversation)
        val historyService = ChatHistory.getInstance()
        if (historyService.removeConversation(conversation.created)) {
          onPerform(historyService.currentConversationId)
        }
      }

      addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          val index = locationToIndex(e.point)
          if (index >= 0) {
            val cellBounds = getCellBounds(index, index)
            if (e.x > cellBounds.x + cellBounds.width - 30) {
              val conversation = model.getElementAt(index)
              val result = Messages.showYesNoDialog(
                "Are you sure you want to delete '${conversation.name}'?",
                "Delete History Item",
                "Delete",
                "Cancel",
                Messages.getQuestionIcon()
              )
              if (result == Messages.YES) {
                listModel.removeElement(conversation)
                val historyService = ChatHistory.getInstance()
                if (historyService.removeConversation(conversation.created)) {
                  onPerform(historyService.currentConversationId)
                }
              }
              e.consume()
            }
            else {
              model.getElementAt(index)?.let { conversation ->
                onPerform(conversation.created)
              }
            }
          }
        }
      })
      addMouseMotionListener(object : MouseAdapter() {
        override fun mouseMoved(e: MouseEvent) {
          val index = locationToIndex(e.point)
          if (index >= 0) {
            val cellBounds = getCellBounds(index, index)
            cursor = if (e.x > cellBounds.x + cellBounds.width - 30) {
              Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }
            else {
              Cursor.getDefaultCursor()
            }
          }
          else {
            cursor = Cursor.getDefaultCursor()
          }
        }
      })
    }
    val scrollPane = JBScrollPane(list)

    JBPopupFactory.getInstance()
      .createComponentPopupBuilder(scrollPane, list)
      .setMovable(true)
      .setResizable(true)
      .setMinSize(JBUI.size(300, 300))
      .createPopup()
      .showInBestPositionFor(e.dataContext)
  }
}

private class HistoryItemRenderer(
  private val onDelete: (ChatConversationInfo) -> Unit,
) : ListCellRenderer<ChatConversationInfo> {

  override fun getListCellRendererComponent(
    list: JList<out ChatConversationInfo>,
    conversation: ChatConversationInfo?,
    index: Int,
    selected: Boolean,
    hasFocus: Boolean,
  ): Component {
    list.selectionMode = ListSelectionModel.SINGLE_SELECTION

    val panel = JPanel(BorderLayout()).apply {
      background = if (selected) list.selectionBackground else list.background
      border = JBUI.Borders.empty(5)
    }
    conversation ?: return panel

    val textPanel = JPanel(BorderLayout()).apply {
      isOpaque = false
      add(JLabel(conversation.name), BorderLayout.NORTH)
      add(JLabel(formatDuration(conversation.lastUpdated)).apply {
        foreground = UIUtil.getContextHelpForeground()
        font = font.deriveFont(font.size - 2f)
      }, BorderLayout.SOUTH)
    }
    panel.add(textPanel, BorderLayout.CENTER)

    // Create a custom panel for the delete button
    val deletePanel = JPanel(BorderLayout()).apply {
      isOpaque = false
      add(JLabel(AllIcons.General.Delete).apply {
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
          override fun mouseClicked(e: MouseEvent) {
            val result = Messages.showYesNoDialog(
              "Are you sure you want to delete '${conversation.name}'?",
              "Delete History Item",
              "Delete",
              "Cancel",
              Messages.getQuestionIcon()
            )
            if (result == Messages.YES) {
              onDelete(conversation)
            }
            e.consume()
          }
        })
      }, BorderLayout.CENTER)
    }
    panel.add(deletePanel, BorderLayout.EAST)

    return panel
  }
}