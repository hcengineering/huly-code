// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.providers.lmstudio

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
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class LMStudioSettingsPanel(val provider: LMStudioProvider) : SettingsPanel {
  private var baseUrlField: JBTextField? = null
  private var connectButton: JButton? = null
  private var statusLabel: JBLabel? = null

  private val description = """
    Run local LLMs. <br>
    To use LM Studio, you need to install the <a href="https://lmstudio.ai">LM Studio</a> and add at least one model.<br>
    To get a model, you can run the following command in the terminal: <code>lms get qwen-2.5-coder-7b</code>
  """
  private val scope = MainScope().plus(CoroutineName("LMStudioSettings"))

  fun updateConnectionStatus() {
    connectButton?.isVisible = !provider.authenticated
    statusLabel?.text = if (provider.authenticated) "Connected" else "Not connected"
    statusLabel?.icon = if (provider.authenticated) AllIcons.General.InspectionsOK else AllIcons.General.InspectionsWarning
  }

  override fun reset() {
    baseUrlField?.text = ChatSettings.getInstance().state.lmsBaseUrl
  }

  override fun apply() {
    ChatSettings.getInstance().state.lmsBaseUrl = baseUrlField?.text
  }

  override fun isModified(): Boolean {
    if (baseUrlField?.text.isNullOrEmpty() && ChatSettings.getInstance().state.lmsBaseUrl.isNullOrEmpty()) {
      return false
    }
    return baseUrlField?.text != ChatSettings.getInstance().state.lmsBaseUrl
  }

  override fun createComponent(): JComponent? {
    val descriptionLabel = JBHtmlPane().apply {
      foreground = JBColor.getColor("Label.infoForeground")
      background = JBColor.PanelBackground
      text = description
    }
    baseUrlField = JBTextField().apply {
      text = ChatSettings.getInstance().state.lmsBaseUrl
      setEmptyState(LM_STUDIO_DEFAULT_API_URL)
    }

    connectButton = JButton("Reconnect").apply {
      addActionListener {
        scope.launch {
          withContext(Dispatchers.IO) {
            connectButton?.isEnabled = false
            try {
              val baseUrl = if (baseUrlField?.text.isNullOrEmpty()) LM_STUDIO_DEFAULT_API_URL else baseUrlField?.text
              provider.fetchModels(baseUrl)
              updateConnectionStatus()
            }
            catch (e: Exception) {
              statusLabel?.text = e.message
            }
            connectButton?.isEnabled = true
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