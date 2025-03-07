// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.ui

import com.hulylabs.intellij.plugins.chat.actions.HistoryAction
import com.hulylabs.intellij.plugins.chat.actions.NewChatAction
import com.hulylabs.intellij.plugins.chat.actions.SelectModelAction
import com.hulylabs.intellij.plugins.chat.actions.SettingsAction
import com.hulylabs.intellij.plugins.chat.api.ChatMessage
import com.hulylabs.intellij.plugins.chat.settings.ChatHistory
import com.hulylabs.intellij.plugins.chat.settings.ChatSettings
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.jcef.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import javax.swing.JPanel

private val LOG = Logger.getInstance("#aichat.toolwindow")

class ChatToolWindowFactory : ToolWindowFactory {
  private lateinit var browser: JBCefBrowser
  private lateinit var headerPanel: ChatHeaderPanel
  private var count = AtomicInteger(0)
  private val messages: MutableList<ChatMessage> = mutableListOf()

  @OptIn(DelicateCoroutinesApi::class)
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val manager = toolWindow.contentManager
    browser = JBCefBrowserBuilder().setEnableOpenDevToolsMenuItem(true).build()

    // Create header panel
    headerPanel = ChatHeaderPanel()
    headerPanel.setTitle("New Chat")
    headerPanel.setTokenCount(0, 20000) // Example values

    // Create main content panel
    val contentPanel = JPanel(BorderLayout())
    contentPanel.add(headerPanel, BorderLayout.NORTH)
    contentPanel.add(browser.component, BorderLayout.CENTER)

    toolWindow.setTitleActions(listOf(
      SelectModelAction {
        updateTokens()
      },
      Separator.getInstance(),
      NewChatAction {
        headerPanel.setTitle("New Chat")
        messages.clear()
        updateTokens()
        postNewChat()
        ChatHistory.getInstance().newConversation()
      },
      HistoryAction { conversationId ->
        loadHistory(conversationId)
      },
      SettingsAction()
    ))

    browser.jbCefClient.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, 100)
    CefApp.getInstance().registerSchemeHandlerFactory("http", "hulychat")
    { _, _, _, _ -> CustomResourceHandler() }
    //browser.openDevtools()
    val postMessageQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    val setRoleQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    val copyCodeQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    postMessageQuery.addHandler(Function { msg: String? ->
      LOG.info("postMessageQuery $msg")
      msg?.let {
        val model = ChatSettings.getInstance().activeLanguageModel
        var message = Json.decodeFromString<ChatMessage>(msg)
        message.id = "${count.incrementAndGet()}"
        // remove last 2 messages if last message was error
        if (messages.size >= 2 && messages.last().isError) {
          messages.removeLast()
          messages.removeLast()
        }
        messages.add(message)
        var systemMessage = ChatMessage("", "assistant", "${count.incrementAndGet()}")
        postNewMessage(message, systemMessage)
        if (model == null) {
          postSystemMessage("No language model is selected", "assistant", true)
        }
        else {
          GlobalScope.launch {
            try {
              model.provider.sendChatRequest(model, messages).collect {
                systemMessage.content += it.content
                systemMessage.role = it.role
                postSystemMessage(it.content, it.role, false)
              }
            }
            catch (e: Exception) {
              LOG.warn(e)
              postSystemMessage("Provider Error: ${e.message}", "assistant", true)
            }
            messages.add(systemMessage)
            ChatHistory.getInstance().updateConversationMessages(messages)
          }
        }
      }
      null
    })
    setRoleQuery.addHandler(Function { msg: String? ->
      LOG.info("setRoleQuery $msg")
      msg?.let {
        val jsonObject = Json.parseToJsonElement(msg).jsonObject
        val messageId = jsonObject["messageId"]!!.jsonPrimitive.content
        val role = jsonObject["role"]!!.jsonPrimitive.content
        messages.find { it.id == messageId }?.let { it.role = role }
      }
      null
    })
    copyCodeQuery.addHandler(Function { msg: String? ->
      msg?.let {
        CopyPasteManager.copyTextToClipboard(msg)
      }
      null
    })

    manager.addContent(
      manager.factory.createContent(contentPanel, null, true)
        .apply { isCloseable = false }
    )

    browser.loadURL("http://hulychat/index.html")
    browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
      override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
        browser.executeJavaScript("""
          window.hulyChat = {};
          window.hulyChat.setChatWindow = function(chatWindow) {};
          window.hulyChat.postMessage = function(message) {
            var msg = JSON.stringify(message);
            ${postMessageQuery.inject("msg")}
          };
          window.hulyChat.setRole = function(messageId, role) {
            var msg = JSON.stringify({messageId, role});
            ${setRoleQuery.inject("msg")}
          };
          window.hulyChat.copyCode = function(text) {
            ${copyCodeQuery.inject("text")}
            return Promise.resolve();
          };
        """, browser.url, 0)
        loadHistory(ChatHistory.getInstance().currentConversationId)
      }
    }, browser.cefBrowser)
  }

  private fun loadHistory(conversationId: Long) {
    val conversation = ChatHistory.getInstance().loadConversationMessages(conversationId)
    headerPanel.setTitle(ChatHistory.getInstance().getConversationTitle(conversationId))
    messages.clear()
    messages.addAll(conversation)
    if (conversation.isNotEmpty()) {
      count.set(messages.maxOf { it.id!!.toInt() } + 1)
    }
    postNewChat()
    for (message in conversation) {
      postAddLoadedMessage(message)
    }
    updateTokens()
  }

  private fun updateTokens() {
    val model = ChatSettings.getInstance().activeLanguageModel
    if (model != null) {
      headerPanel.setTokenCount(model.provider.getTokenCount(messages), model.maxContextLength)
    }
  }

  private fun sendToWebView(type: String, vararg args: String) {
    var argsStr = args.joinToString(separator = ", ")
    if (argsStr.isNotEmpty()) {
      argsStr = ", $argsStr"
    }
    val js = """
      window.postMessage({ type: "$type" $argsStr });
    """.trimIndent()
    LOG.info("sendToWebView $js")
    browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
  }

  private fun postNewMessage(message: ChatMessage, systemMessage: ChatMessage) {
    sendToWebView("new-user-message",
                  "message: ${Json.encodeToString(message)}",
                  "systemMessage: ${Json.encodeToString(systemMessage)}")
  }

  private fun postNewChat() {
    sendToWebView("new-chat")
  }

  private fun postAddLoadedMessage(message: ChatMessage) {
    sendToWebView("add-loaded-message",
                  "message: ${Json.encodeToString(message)}")
  }

  private fun postSystemMessage(content: String, role: String, isError: Boolean = false) {
    sendToWebView("model-message",
                  "content: ${Json.encodeToString(content)}",
                  "role: \"$role\"",
                  "isError: $isError")
  }
}