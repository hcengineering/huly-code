// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.util.NlsContexts
import org.jetbrains.annotations.NonNls
import javax.swing.JComponent

class ChatSettingsConfigurable : SearchableConfigurable, Configurable {
  private var settingsPanel: ChatSettingsPanel? = null
  override fun getId(): @NonNls String = "preferences.aichat"

  override fun getDisplayName(): @NlsContexts.ConfigurableName String? = "AI Chat"

  override fun createComponent(): JComponent? {
    settingsPanel = ChatSettingsPanel()
    return settingsPanel!!
  }

  override fun isModified(): Boolean {
    return settingsPanel?.isModified() == true
  }

  override fun apply() {
    settingsPanel?.apply()
  }

  override fun reset() {
    settingsPanel?.reset()
  }

  override fun disposeUIResources() {
    settingsPanel = null
  }
}