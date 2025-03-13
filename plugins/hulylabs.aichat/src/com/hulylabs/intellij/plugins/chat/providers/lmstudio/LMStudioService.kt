// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.providers.lmstudio

import com.hulylabs.intellij.plugins.chat.api.ChatMessage
import com.hulylabs.intellij.plugins.chat.settings.ChatSettings
import com.intellij.util.io.HttpRequests
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

const val LM_STUDIO_DEFAULT_API_URL = "http://localhost:1234/api/v0"

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LMStudioModel(
  val id: String,
  val type: String,
  @JsonNames("max_context_length")
  val maxContextLength: Int,
)

@Serializable
data class LMStudioModels(
  val data: List<LMStudioModel>,
)

@Serializable
data class LMStudioChatMessage(
  val role: String,
  val content: String,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LMStudioChatRequest(
  val model: String,
  val messages: List<LMStudioChatMessage>,
  val stream: Boolean,
  @JsonNames("max_tokens")
  val maxTokens: Int,
  val stop: List<String>,
  val temperature: Float,
  val tools: List<String>,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LMStudioChatResponse(
  val id: String,
  val `object`: String,
  val created: Long,
  val model: String,
  @JsonNames("system_fingerprint")
  val systemFingerprint: String,
  val choices: List<LMStudioChoiceDelta>,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class LMStudioChoiceDelta(
  val index: Int,
  val delta: JsonObject? = null,
  @JsonNames("finish_reason")
  val finishReason: String? = null,
)

object LMStudioService {
  val json = Json {
    ignoreUnknownKeys = true
  }

  val apiUrl: String
    get() {
      return ChatSettings.getInstance().state.lmsBaseUrl ?: LM_STUDIO_DEFAULT_API_URL
    }

  fun getModels(): List<LMStudioModel> {
    val uri = "${apiUrl}/models"
    val response = HttpRequests.request(uri).accept(HttpRequests.JSON_CONTENT_TYPE).readString()
    val models = json.decodeFromString<LMStudioModels>(response)
    return models.data.filter { it.type != "embeddings" }
  }

  fun loadModel(model: String) {
    val uri = "${apiUrl}/completions"
    val request = """
        {
            "model": ${Json.encodeToString(model)},
            "messages": [],
            "stream": false,
            "max_tokens": 0,
        }
    """
    try {
      HttpRequests
        .post(uri, HttpRequests.JSON_CONTENT_TYPE)
        .accept(HttpRequests.JSON_CONTENT_TYPE)
        .connectTimeout(1000)
        .write(request)
    }
    catch (_: Exception) {
      // can be ignored
    }
  }

  fun sendChatRequest(model: String, messages: List<ChatMessage>): Flow<ChatMessage> {
    val uri = "${apiUrl}/chat/completions"
    val request = LMStudioChatRequest(model, messages.map { LMStudioChatMessage(it.role, it.content) }, true, -1, emptyList(), 0.0f, emptyList())
    val jsonRequest = json.encodeToString(request)
    return callbackFlow {
      HttpRequests
        .post(uri, HttpRequests.JSON_CONTENT_TYPE)
        .accept(HttpRequests.JSON_CONTENT_TYPE)
        .isReadResponseOnError(true)
        .connectTimeout(100000)
        .connect { request ->
          request.write(jsonRequest)
          for (line in request.reader.lines()) {
            val line = line.removePrefix("data: ")
            if (line.isEmpty()) {
              continue
            }
            if (line == "[DONE]") {
              break
            }
            else {
              val response = json.decodeFromString<LMStudioChatResponse>(line)
              if (response.choices.isNotEmpty()) {
                val delta = response.choices.first().delta
                if (delta != null) {
                  val role = delta["role"]?.jsonPrimitive?.content ?: "assistant"
                  val content = delta["content"]?.jsonPrimitive?.content ?: ""
                  trySend(ChatMessage(content, role))
                }
              }
            }
          }
          close()
        }
    }
  }
}