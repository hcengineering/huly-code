// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.settings

import com.hulylabs.intellij.plugins.langconfigurator.messages.HulyLangConfiguratorBundle.message
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class Configuration(val project: Project) : Configurable {
  private var settingsComponent: SettingsComponent? = null

  override fun createComponent(): JComponent? {
    settingsComponent = SettingsComponent()
    return settingsComponent?.panel
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return settingsComponent?.getPreferredFocusedComponent()
  }

  override fun isModified(): Boolean {
    val state =project.service<Settings>().state
    return settingsComponent!!.formatOnSave != state.formatOnSave
  }

  override fun apply() {
    val state =project.service<Settings>().state
    state.formatOnSave =settingsComponent!!.formatOnSave
  }

  override fun reset() {
    val state =project.service<Settings>().state
    settingsComponent!!.formatOnSave = state.formatOnSave
  }

  override fun getDisplayName(): String {
    return message("settings.display.name")
  }
}