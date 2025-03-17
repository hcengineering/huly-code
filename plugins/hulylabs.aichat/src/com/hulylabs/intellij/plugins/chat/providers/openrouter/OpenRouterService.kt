// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.providers.openrouter

import com.hulylabs.intellij.plugins.chat.api.ChatMessage
import com.hulylabs.intellij.plugins.chat.api.LanguageModel
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.io.HttpRequests
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists


private const val OPEN_ROUTER_API_URL = "https://openrouter.ai/api/v1"

@Serializable
data class OpenRouterChatRequest(
  val model: String,
  val messages: List<OpenRouterChatMessage>,
  val stream: Boolean,
)

@Serializable
data class OpenRouterChatMessage(
  val role: String,
  val content: String,
)

@Serializable
data class OpenRouterChatResponse(
  val id: String,
  val choices: List<OpenRouterChoiceDelta>,
  val usage: JsonObject? = null,
)

@Serializable
data class OpenRouterChoiceDelta(
  val index: Int,
  val delta: JsonObject? = null,
)

@Service(Service.Level.APP)
class OpenRouterService {
  private val MODELS_CACHE_FILE = PathManager.getSystemDir().resolve("hulychat").resolve("openrouter_models.json")
  private val models: MutableList<OpenRouterModel> = mutableListOf()
  private val scope = MainScope().plus(CoroutineName("OpenRouter"))
  private var apiKey: String? = null
  private var tokensCount = 0

  val json = Json {
    ignoreUnknownKeys = true
  }

  init {
    if (!ApplicationManager.getApplication().isHeadlessEnvironment) {
      scope.launch {
        withContext(Dispatchers.IO) {
          loadModels()
          val passwordSafe = PasswordSafe.Companion.instance
          val attributes = CredentialAttributes(
            generateServiceName("HulyChat", "OpenRouterApiKey")
          )
          apiKey = passwordSafe.getPassword(attributes)
        }
      }
    }
  }

  fun updateApiKey(key: String?) {
    apiKey = key
  }

  fun getModels(): List<OpenRouterModel> {
    return models
  }

  fun loadModels(force: Boolean = false) {
    models.clear()
    if (!force && Files.exists(MODELS_CACHE_FILE)) {
      models.addAll(json.decodeFromString(Files.readString(MODELS_CACHE_FILE)))
    }
    else {
      val uri = "${OPEN_ROUTER_API_URL}/models"
      val response = HttpRequests.request(uri).accept(HttpRequests.JSON_CONTENT_TYPE).readString()
      val modelsJson: JsonElement = json.decodeFromString(response)
      models.addAll(json.decodeFromJsonElement<List<OpenRouterModel>>(modelsJson.jsonObject["data"]!!))
      val dir = MODELS_CACHE_FILE.parent
      if (dir.notExists()) {
        dir.createDirectories()
      }
      MODELS_CACHE_FILE.toFile().writeText(json.encodeToString(models), Charsets.UTF_8)
    }
  }

  fun getTokenCount(): Int {
    return tokensCount
  }

  fun sendChatRequest(model: LanguageModel, messages: List<ChatMessage>): Flow<ChatMessage> {
    val uri = "${OPEN_ROUTER_API_URL}/chat/completions"
    val request = OpenRouterChatRequest(model.id, messages.map { OpenRouterChatMessage(it.role, it.content) }, true)
    val jsonRequest = json.encodeToString(request)
    return callbackFlow {
      HttpRequests
        .post(uri, HttpRequests.JSON_CONTENT_TYPE)
        .accept(HttpRequests.JSON_CONTENT_TYPE)
        .isReadResponseOnError(true)
        .connectTimeout(100000)
        .tuner { tuner ->
          tuner.setRequestProperty("Authorization", "Bearer ${apiKey}")
          tuner.setRequestProperty("HTTP-Referer", "https://hulylabs.com/")
          tuner.setRequestProperty("X-Title", "HulyCode")
        }
        .connect { request ->
          request.write(jsonRequest)
          for (line in request.reader.lines()) {
            val line = line.removePrefix("data: ")
            if (line.isEmpty()) {
              continue
            }
            if (line.startsWith(": OPENROUTER PROCESSING")) {
              continue
            }
            if (line == "[DONE]") {
              break
            }
            else {
              val response = json.decodeFromString<OpenRouterChatResponse>(line)
              response.usage?.let {
                it["total_tokens"]?.jsonPrimitive?.content?.toInt()?.let { totalTokens ->
                  tokensCount = totalTokens
                }
              }
              if (response.choices.isNotEmpty()) {
                val choice = response.choices.first()
                if (choice.delta != null) {
                  val role = choice.delta["role"]?.jsonPrimitive?.content ?: "assistant"
                  val reasoningObj = choice.delta["reasoning"]
                  var reasoning = ""
                  if (reasoningObj != null && reasoningObj !is JsonNull) {
                    reasoning = reasoningObj.jsonPrimitive.content
                  }
                  val content = choice.delta["content"]?.jsonPrimitive?.content ?: ""
                  trySend(ChatMessage(if (content.isEmpty()) reasoning else content, role))
                }
              }
            }
          }
          close()
        }
    }
  }

  companion object {
    fun getInstance(): OpenRouterService {
      return service()
    }
  }
}