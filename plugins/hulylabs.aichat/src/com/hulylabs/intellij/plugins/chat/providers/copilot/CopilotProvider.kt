// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.providers.copilot

import com.hulylabs.intellij.plugins.chat.api.ChatMessage
import com.hulylabs.intellij.plugins.chat.api.LanguageModel
import com.hulylabs.intellij.plugins.chat.api.LanguageModelProvider
import com.hulylabs.intellij.plugins.chat.api.SettingsPanel
import com.hulylabs.intellij.plugins.completion.providers.copilot.AgentStatus
import com.hulylabs.intellij.plugins.completion.providers.copilot.lsp.AuthStatusKind
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBHtmlPane
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.awt.FlowLayout
import java.util.concurrent.CompletableFuture
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class CopilotProvider : LanguageModelProvider {
  override val id: String = "copilot"
  override val name: String = "GitHub Copilot"
  override val enabled: Boolean = true
  private val description = """
    To use GitHub Copilot in Huly Code Chat, you need to be logged in to GitHub.<br>
    Note that your GitHub account must have an active Copilot Chat subscription.
    """
  private val scope = MainScope().plus(CoroutineName("Copilot"))

  private val providedModels: List<LanguageModel> = listOf(
    LanguageModel(this, "gpt-4o-2024-05-13", "GPT-4o", 64_000),
    LanguageModel(this, "gpt-4", "GPT-4", 32_768),
    LanguageModel(this, "gpt-3.5-turbo", "GPT-3.5", 12_288),
    LanguageModel(this, "o1", "o1", 20_000, useStreaming = false),
    LanguageModel(this, "o1-mini", "o1-mini", 20_000, useStreaming = false),
    LanguageModel(this, "claude-3.5-sonnet", "Claude 3.5 Sonnet", 200_000),
    LanguageModel(this, "claude-3.7-sonnet", "Claude 3.7 Sonnet", 90_000),
    LanguageModel(this, "gemini-2.0-flash-001", "Gemini 2.0 Flash", 128_000),
  )
  private val copilot = CopilotService()

  override val authenticated: Boolean
    get() {
      return copilot.isAuthenticated()
    }

  override fun authenticate(): CompletableFuture<Unit> {
    if (copilot.isAuthenticated()) {
      return CompletableFuture.completedFuture(Unit)
    }
    // reuse copilot service from completion plugin
    val project = ProjectManager.getInstance().openProjects.firstOrNull()
                  ?: return CompletableFuture.failedFuture(Exception("No project opened"))
    val copilotService = com.hulylabs.intellij.plugins.completion.providers.copilot.CopilotService.getInstance(project)
    var wasStarted = copilotService.getAgentStatus() == AgentStatus.Started
    val result = CompletableFuture<Unit>()
    scope.launch {
      withContext(Dispatchers.IO) {
        if (copilotService.getAgentStatus() == AgentStatus.Stopped) {
          wasStarted = false
          copilotService.start()
        }
        var count = 0
        while (copilotService.getAgentStatus() != AgentStatus.Started && count < 100) {
          delay(1000)
          count++
        }
        if (copilotService.getAgentStatus() != AgentStatus.Started) {
          result.completeExceptionally(Exception("Failed to start Copilot service"))
          return@withContext
        }
        copilotService.signIn()
        count = 0
        while (copilotService.getAuthStatus() == AuthStatusKind.NotSignedIn && count < 100) {
          delay(1000)
          count++
          println(copilotService.getAuthStatus())
        }
        val authStatus = copilotService.getAuthStatus()
        if (!wasStarted) { // stop service in case if we start it
          copilotService.stop()
        }
        if (authStatus != AuthStatusKind.OK) {
          result.completeExceptionally(Exception("Failed to sign: ${authStatus}"))
        }
        else {
          copilot.fetchAuthToken()
          result.complete(Unit)
        }
      }
    }
    return result
  }

  override fun getTokenCount(request: List<ChatMessage>): Int {
    return copilot.usageTokens
  }

  override fun providedModels(): List<LanguageModel> = providedModels

  override suspend fun sendChatRequest(model: LanguageModel, request: List<ChatMessage>): Flow<ChatMessage> {
    return copilot.sendChatRequest(model, request)
  }

  override fun createSettingsPanel(): SettingsPanel {
    return object : SettingsPanel {
      private var authenticateButton: JButton? = null
      private var statusLabel: JBLabel? = null

      fun updateConnectionStatus(e: Throwable? = null) {
        authenticateButton?.isVisible = !authenticated
        statusLabel?.text = if (authenticated) "Authorised" else if (e != null) e.message else "Not authorised"
        statusLabel?.icon = if (authenticated) AllIcons.General.InspectionsOK else if (e != null) AllIcons.General.InspectionsWarning else AllIcons.General.InspectionsError
      }

      override fun createComponent(): JComponent? {
        val descriptionLabel = JBHtmlPane().apply {
          foreground = JBColor.getColor("Label.infoForeground")
          background = JBColor.background()
          text = description
        }
        authenticateButton = JButton("Sign In").apply {
          addActionListener {
            authenticateButton?.isEnabled = false
            statusLabel?.text = "Processing..."
            statusLabel?.icon = AllIcons.General.Web
            authenticate().whenComplete { v, e ->
              authenticateButton?.isEnabled = true
              updateConnectionStatus(e)
            }
          }
        }
        statusLabel = JBLabel("Authorised")
        val formBuilder = FormBuilder.createFormBuilder()
          .addComponent(descriptionLabel)
          .addComponent(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(authenticateButton)
            add(statusLabel)
          })
        updateConnectionStatus()
        return formBuilder.panel
      }
    }
  }
}