// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.providers.lmstudio

import com.hulylabs.intellij.plugins.chat.api.ChatMessage
import com.hulylabs.intellij.plugins.chat.api.LanguageModel
import com.hulylabs.intellij.plugins.chat.api.LanguageModelProvider
import com.hulylabs.intellij.plugins.chat.api.SettingsPanel
import com.hulylabs.intellij.plugins.chat.settings.ChatSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.setEmptyState
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBHtmlPane
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.awt.FlowLayout
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class LMStudioProvider : LanguageModelProvider {
  override val id: String = "lmstudio"
  override val name: String = "LM Studio"
  override val enabled: Boolean = true

  private val description = """
    Run local LLMs. <br>
    To use LM Studio, you need to install the <a href="https://lmstudio.ai">LM Studio</a> and add at least one model.<br>
    To get a model, you can run the following command in the terminal: <code>lms get qwen-2.5-coder-7b</code>
  """

  private val availableModels = Collections.synchronizedList(mutableListOf<LanguageModel>())
  private val scope = MainScope().plus(CoroutineName("LMStudio"))

  override val authenticated: Boolean
    get() {
      return availableModels.isNotEmpty()
    }

  init {
    try {
      // try to authenticate on startup without error reporting
      authenticate()
    }
    catch (_: Exception) {
      // ignore
    }
  }

  override fun authenticate(): CompletableFuture<Unit> {
    val result = CompletableFuture<Unit>()
    if (availableModels.isEmpty()) {
      scope.launch(Dispatchers.IO) {
        try {
          fetchModels()
          result.complete(Unit)
        }
        catch (e: Exception) {
          result.completeExceptionally(e)
        }
      }
    }
    else {
      result.complete(Unit)
    }
    return result
  }

  override fun providedModels(): List<LanguageModel> {
    return availableModels
  }

  override fun loadModel(model: LanguageModel) {
    LMStudioService.loadModel(model.id)
  }

  override fun getTokenCount(request: List<ChatMessage>): Int {
    return request.sumOf { it.content.count { it.isWhitespace() } + 1 }
  }

  override suspend fun sendChatRequest(model: LanguageModel, request: List<ChatMessage>): Flow<ChatMessage> {
    return LMStudioService.sendChatRequest(model.id, request)
  }

  override fun createSettingsPanel(): SettingsPanel {
    return object : SettingsPanel {
      private var baseUrlField: JBTextField? = null
      private var connectButton: JButton? = null
      private var statusLabel: JBLabel? = null

      fun updateConnectionStatus() {
        connectButton?.isVisible = !authenticated
        statusLabel?.text = if (authenticated) "Connected" else "Not connected"
        statusLabel?.icon = if (authenticated) AllIcons.General.InspectionsOK else AllIcons.General.InspectionsWarning
      }

      override fun reset() {
        baseUrlField?.text = ChatSettings.getInstance().state.lmsBaseUrl
      }

      override fun apply() {
        ChatSettings.getInstance().state.lmsBaseUrl = baseUrlField?.text
      }

      override fun isModified(): Boolean {
        return baseUrlField?.text != ChatSettings.getInstance().state.lmsBaseUrl
      }

      override fun createComponent(): JComponent? {
        val descriptionLabel = JBHtmlPane().apply {
          foreground = JBColor.getColor("Label.infoForeground")
          background = JBColor.background()
          text = description
        }
        baseUrlField = JBTextField().apply {
          text = ChatSettings.getInstance().state.lmsBaseUrl
          setEmptyState(LM_STUDIO_DEFAULT_API_URL)
        }

        connectButton = JButton("Reconnect").apply {
          addActionListener {
            authenticate().whenComplete { v, e ->
              updateConnectionStatus()
              if (e != null) {
                statusLabel?.text = e.message
              }
            }
          }
        }

        statusLabel = JBLabel("Connected")
        statusLabel!!.icon = AllIcons.General.InspectionsOK

        val formBuilder = FormBuilder.createFormBuilder()
          .addComponent(descriptionLabel)
          .addLabeledComponent("Base URL:", baseUrlField!!)
          .addComponent(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(connectButton)
            add(statusLabel)
          })
        updateConnectionStatus()
        return formBuilder.panel
      }
    }
  }

  private fun fetchModels() {
    val models = LMStudioService.getModels().map { LanguageModel(this@LMStudioProvider, it.id, it.id.take(30), it.maxContextLength) }.sortedBy { it.id }
    availableModels.clear()
    availableModels.addAll(models)
  }
}