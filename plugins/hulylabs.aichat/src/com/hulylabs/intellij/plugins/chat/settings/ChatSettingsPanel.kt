// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.settings

import com.hulylabs.intellij.plugins.chat.api.SettingsPanel
import com.hulylabs.intellij.plugins.chat.providers.LanguageModelProviderRegistry
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel

class ChatSettingsPanel : JPanel(BorderLayout()) {
  private val providerPanels = mutableMapOf<String, SettingsPanel>()

  init {
    val mainPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      border = JBUI.Borders.empty(10)
    }

    // Create panels for each provider
    LanguageModelProviderRegistry.getInstance().getProviders().forEach { provider ->
      val providerSettingsPanel = provider.createSettingsPanel()
      providerPanels[provider.id] = providerSettingsPanel
      val providerPanel = JPanel(BorderLayout()).apply {
        alignmentX = LEFT_ALIGNMENT
        border = IdeBorderFactory.createTitledBorder(provider.name, true)
      }
      providerPanel.add(providerSettingsPanel.createComponent()!!, BorderLayout.CENTER)
      mainPanel.add(providerPanel)
      mainPanel.add(Box.createVerticalStrut(10))
    }
    mainPanel.add(Box.createVerticalGlue())
    val wrapperPanel = JPanel(BorderLayout()).apply {
      add(mainPanel, BorderLayout.NORTH)
    }
    add(wrapperPanel, BorderLayout.CENTER)
  }

  fun isModified(): Boolean {
    return providerPanels.values.any { it.isModified() }
  }

  fun apply() {
    providerPanels.values.forEach { it.apply() }
  }

  fun reset() {
    providerPanels.values.forEach { it.reset() }
  }
}