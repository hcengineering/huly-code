// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.providers.copilot

import com.hulylabs.intellij.plugins.chat.api.SettingsPanel
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBHtmlPane
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class CopilotSettingsPanel(val provider: CopilotProvider) : SettingsPanel {
  private var authenticateButton: JButton? = null
  private var statusLabel: JBLabel? = null
  private val description = """
    To use GitHub Copilot in Huly Code Chat, you need to be logged in to GitHub.<br>
    Note that your GitHub account must have an active Copilot Chat subscription.
    """

  fun updateConnectionStatus(e: Throwable? = null) {
    authenticateButton?.isVisible = !provider.authenticated
    statusLabel?.text = if (provider.authenticated) "Authorised" else if (e != null) e.message else "Not authorised"
    statusLabel?.icon = if (provider.authenticated) AllIcons.General.InspectionsOK else if (e != null) AllIcons.General.InspectionsWarning else AllIcons.General.InspectionsError
  }

  override fun createComponent(): JComponent? {
    val descriptionLabel = JBHtmlPane().apply {
      foreground = JBColor.getColor("Label.infoForeground")
      background = JBColor.PanelBackground
      text = description
    }
    authenticateButton = JButton("Sign In").apply {
      addActionListener {
        authenticateButton?.isEnabled = false
        statusLabel?.text = "Processing..."
        statusLabel?.icon = AllIcons.General.Web
        provider.authenticate().whenComplete { v, e ->
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