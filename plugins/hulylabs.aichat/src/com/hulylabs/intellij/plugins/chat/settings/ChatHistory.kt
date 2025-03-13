// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.settings

import com.hulylabs.intellij.plugins.chat.api.ChatMessage
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText

@Serializable
data class ChatConversationInfo(
  var name: String,
  var created: Long,
  var lastUpdated: Long,
)

private val LOG = Logger.getInstance("#aichat.history")

@Service(Service.Level.APP)
class ChatHistory {
  private val storageDir = PathManager.getSystemDir().resolve("hulychat")
  private val historyFile = storageDir.resolve("history.json")
  private val json = Json {
    prettyPrint = true
  }
  private var conversations: MutableMap<Long, ChatConversationInfo> = mutableMapOf()
  var currentConversationId = 0L

  companion object {
    @JvmStatic
    fun getInstance(): ChatHistory = service()
  }

  init {
    if (!storageDir.exists()) {
      Files.createDirectories(storageDir)
    }

    if (historyFile.exists()) {
      try {
        conversations = json.decodeFromString<MutableMap<Long, ChatConversationInfo>>(historyFile.readText(Charsets.UTF_8))
      }
      catch (e: Exception) {
        LOG.error(e)
      }
    }
    if (conversations.isEmpty()) {
      newConversation()
    } else {
      currentConversationId = conversations.values.maxBy { it.lastUpdated }.created
    }
  }

  fun newConversation() {
    val now = System.currentTimeMillis()
    currentConversationId = now
    conversations[now] = ChatConversationInfo("New Chat", now, now)
    storeInfos()
  }

  fun updateConversationTitle(name: String) {
    conversations[currentConversationId]?.let {
      it.name = name
      storeInfos()
    }
  }

  fun getConversationTitle(conversationId: Long): String {
    return conversations[conversationId]?.name ?: "New Chat"
  }

  fun updateConversationMessages(messages: List<ChatMessage>) {
    conversations[currentConversationId]?.let {
      it.lastUpdated = System.currentTimeMillis()
      storeInfos()
      try {
        storageDir.resolve("conversation_${it.created}.json").toFile().writeText(json.encodeToString(messages), Charsets.UTF_8)
      }
      catch (e: Exception) {
        LOG.error(e)
      }
    }
  }

  fun history(): List<ChatConversationInfo> {
    return conversations.values.sortedBy { -it.lastUpdated }.toList()
  }

  fun loadConversationMessages(conversationId: Long): List<ChatMessage> {
    currentConversationId = conversationId
    val conversationFile = storageDir.resolve("conversation_${conversationId}.json")
    if (conversationFile.exists()) {
      try {
        return json.decodeFromString<List<ChatMessage>>(conversationFile.readText(Charsets.UTF_8)).sortedBy { it.id }
      }
      catch (e: Exception) {
        LOG.error(e)
      }
    }
    return emptyList()
  }

  private fun storeInfos() {
    try {
      historyFile.toFile().writeText(json.encodeToString(conversations), Charsets.UTF_8)
    }
    catch (e: Exception) {
      LOG.error(e)
    }
  }
}
