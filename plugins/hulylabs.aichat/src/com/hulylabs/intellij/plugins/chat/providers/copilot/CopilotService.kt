// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.providers.copilot

import com.hulylabs.intellij.plugins.chat.api.ChatMessage
import com.hulylabs.intellij.plugins.chat.api.LanguageModel
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.SystemProperties
import com.intellij.util.io.HttpRequests
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection
import kotlin.io.path.exists
import kotlin.io.path.readText

data class ApiToken(
  val token: String,
  val expiresAt: Long,
)

private const val COPILOT_CHAT_COMPLETION_URL = "https://api.githubcopilot.com/chat/completions"
private const val COPILOT_CHAT_AUTH_URL = "https://api.github.com/copilot_internal/v2/token"

// 5 minutes threshold
private const val API_TOKEN_EXPIRE_THRESHOLD_MS = 300_000

@Serializable
data class CopilotChatRequest(
  val intent: Boolean,
  val n: Int,
  val stream: Boolean,
  val temperature: Float,
  val model: String,
  val messages: List<CopilotChatMessage>,
)

@Serializable
data class CopilotChatMessage(
  val role: String,
  val content: String,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class CopilotResponseChoice(
  val index: Long,
  @JsonNames("finish_reason")
  val finishReason: String? = null,
  val delta: ResponseDelta? = null,
  val message: ResponseDelta? = null,
)

@Serializable
data class ResponseDelta(
  val content: String? = null,
  val role: String? = null,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class Usage(
  @JsonNames("completion_tokens")
  val completionTokens: Int,
  @JsonNames("prompt_tokens")
  val promptTokens: Int,
  @JsonNames("total_tokens")
  val totalTokens: Int,
)

@Serializable
data class CopilotChatResponse(
  val choices: List<CopilotResponseChoice>,
  val created: Long,
  val id: String,
  val usage: Usage? = null,
)

class CopilotService {
  private var authToken: String? = null
  private var apiToken: ApiToken? = null
  var usageTokens: Int = 0
  var lastResponseRole: String = "assistant"

  val json = Json {
    ignoreUnknownKeys = true

  }
  val copilotConfigDirPath: Path
  val processingCancelled = AtomicBoolean(false)

  init {
    if (SystemInfo.isWindows) {
      val localAppData = Path.of(System.getenv("LOCALAPPDATA")) ?: Path.of(SystemProperties.getUserHome(), "AppData", "Local")
      copilotConfigDirPath = localAppData.resolve("github-copilot")
    }
    else {
      copilotConfigDirPath = Path.of(SystemProperties.getUserHome(), ".config", "github-copilot")
    }
    fetchAuthToken()
  }

  fun fetchAuthToken() {
    for (file in arrayOf(copilotConfigDirPath.resolve("hosts.json"), copilotConfigDirPath.resolve("apps.json"))) {
      if (file.exists()) {
        val json = Json.parseToJsonElement(file.readText())
        if (json is JsonObject) {
          for ((key, value) in json.jsonObject.entries) {
            if (key.startsWith("github.com")) {
              val authToken = value.jsonObject["oauth_token"]?.jsonPrimitive?.content
              if (authToken != null) {
                this.authToken = authToken
                return
              }
            }
          }
        }
      }
    }
  }

  private fun requestApiToken(): ApiToken? {
    try {
      val response = HttpRequests.request(COPILOT_CHAT_AUTH_URL)
        .accept(HttpRequests.JSON_CONTENT_TYPE)
        .isReadResponseOnError(true)
        .connectTimeout(1000)
        .tuner { connection ->
          connection.setRequestProperty("Authorization", "token ${authToken}")
        }.readString()
      val json = Json.parseToJsonElement(response)
      if (json is JsonObject) {
        val token = json.jsonObject["token"]?.jsonPrimitive?.content
        val expiresAt = json.jsonObject["expires_at"]?.jsonPrimitive?.longOrNull
        if (token != null && expiresAt != null) {
          return ApiToken(token, expiresAt * 1000)
        }
      }
    }
    catch (_: Exception) {
      // can be ignored
    }
    return null
  }

  fun isAuthenticated(): Boolean {
    return authToken != null
  }

  fun sendChatRequest(model: LanguageModel, messages: List<ChatMessage>): Flow<ChatMessage> {
    processingCancelled.set(false)
    if (apiToken == null || apiToken!!.expiresAt < System.currentTimeMillis() - API_TOKEN_EXPIRE_THRESHOLD_MS) {
      apiToken = requestApiToken()
    }
    if (apiToken == null) {
      throw Exception("Failed to get API token")
    }
    val jsonRequest = json.encodeToString(
      CopilotChatRequest(true, 1, model.useStreaming, 0.1f, model.id,
                         messages.map { CopilotChatMessage(it.role, it.content) }))
    return callbackFlow {
      HttpRequests
        .post(COPILOT_CHAT_COMPLETION_URL, HttpRequests.JSON_CONTENT_TYPE)
        .accept(HttpRequests.JSON_CONTENT_TYPE)
        .isReadResponseOnError(true)
        .connectTimeout(100000)
        .tuner { connection ->
          connection.setRequestProperty("Editor-Version", "HulyCode/251")
          connection.setRequestProperty("Authorization", "Bearer ${apiToken!!.token}")
          connection.setRequestProperty("Copilot-Integration-Id", "vscode-chat")
        }
        .connect { request ->
          request.write(jsonRequest)
          val connection = request.connection

          if (connection is HttpsURLConnection && connection.responseCode != 200) {
            val errorResponse = request.readError()
            throw IOException("Error: ${connection.responseCode}: ${errorResponse ?: "unknown"}")
          }
          if (model.useStreaming) {
            for (line in request.reader.lines()) {
              if (processingCancelled.get()) {
                break
              }
              val line = line.removePrefix("data: ")
              if (line.isEmpty()) {
                continue
              }
              if (line == "[DONE]") {
                break
              }
              else {
                val response = json.decodeFromString<CopilotChatResponse>(line)
                response.usage?.let { usage ->
                  usageTokens = usage.totalTokens
                }
                if (response.choices.isEmpty() || response.choices.first().finishReason != null) {
                  continue
                }
                response.choices.first().delta?.role?.let { lastResponseRole = it }
                val content = response.choices.first().delta?.content ?: continue
                trySend(ChatMessage(content, lastResponseRole))
              }
            }
          }
          else {
            var responseText = request.reader.readText()
            val response = json.decodeFromString<CopilotChatResponse>(responseText)
            response.usage?.let { usage ->
              usageTokens = usage.totalTokens
            }
            if (response.choices.isNotEmpty() && response.choices.first().finishReason == null) {
              response.choices.first().delta?.role?.let { lastResponseRole = it }
              response.choices.first().message?.content?.let { trySend(ChatMessage(it, lastResponseRole)) }
            }
          }
          close()
        }
    }
  }

  fun cancelProcessing() {
    processingCancelled.set(true)
  }
}