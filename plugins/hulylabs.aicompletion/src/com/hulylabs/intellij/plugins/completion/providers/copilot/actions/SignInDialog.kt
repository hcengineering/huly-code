// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.copilot.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class SignInDialog(
  project: Project,
  private val deviceCode: String,
  private val verificationUri: String,
) : DialogWrapper(project, false) {

  private lateinit var spinnerPanel: JPanel

  init {
    title = "Connect to GitHub"
    isResizable = false
    isModal = false
    okAction.putValue(Action.NAME, "Connect to GitHub")
    init()
    pack()
  }

  override fun createActions(): Array<out Action?> {
    return arrayOf(okAction)
  }

  override fun doOKAction() {
    spinnerPanel.isVisible = true
    BrowserUtil.open(verificationUri)
  }

  override fun createCenterPanel(): JComponent? {
    val panel = JPanel()
    panel.layout = GridBagLayout()
    val gb = GridBagConstraints()

    gb.apply {
      insets = JBUI.insets(8)
      weightx = 1.0
      weighty = 0.0
      gridwidth = GridBagConstraints.REMAINDER
      gridx = 0
      gridy = 0
      anchor = GridBagConstraints.WEST
      fill = GridBagConstraints.HORIZONTAL
    }
    val headerLabel = JLabel("Use GitHub Copilot in Huly Code.")
    headerLabel.font = headerLabel.font.deriveFont(headerLabel.font.size + 4f)
    panel.add(headerLabel, gb)

    gb.apply {
      gridy = 1
      insets = JBUI.insets(8, 8, 16, 8) // Extra bottom padding
    }
    val subscriptionLabel = JLabel("Using Copilot required an active subscription in GitHub.")
    panel.add(subscriptionLabel, gb)

    val codePanel = JPanel(GridBagLayout())
    val codeGb = GridBagConstraints()

    codeGb.apply {
      insets = JBUI.insets(8)
      weightx = 1.0
      fill = GridBagConstraints.HORIZONTAL
      anchor = GridBagConstraints.CENTER
    }
    val codeField = JTextField(deviceCode)
    codeField.apply {
      isEditable = false
      background = null
      border = null
      font = font.deriveFont(font.size + 6f)
    }
    codePanel.border = BorderFactory.createLineBorder(JBColor.GRAY)
    codePanel.add(codeField, codeGb)

    codeGb.apply {
      weightx = 0.0
      fill = GridBagConstraints.NONE
    }
    val copyButton = JButton("Copy")
    copyButton.addActionListener {
      CopyPasteManager.copyTextToClipboard(deviceCode)
      copyButton.text = "Copied!"
    }
    codePanel.add(copyButton, codeGb)

    gb.apply {
      gridy = 2
      insets = JBUI.insets(0, 8, 16, 8)
    }
    panel.add(codePanel, gb)

    gb.apply {
      gridy = 3
      insets = JBUI.insets(8, 8, 24, 8)
    }
    val instructionsLabel = JLabel("Paste this code into GitHub after clicking the button below")
    panel.add(instructionsLabel, gb)

    // Spinner section
    gb.apply {
      gridy = 4
      insets = JBUI.insets(16, 8, 8, 8)
    }

    spinnerPanel = JPanel(GridBagLayout())
    val spinner = AsyncProcessIcon("Signing in")
    val waitingLabel = JLabel("Waiting response...")

    spinnerPanel.add(spinner)
    spinnerPanel.add(Box.createHorizontalStrut(10))
    spinnerPanel.add(waitingLabel)
    panel.add(spinnerPanel, gb)

    spinnerPanel.isVisible = false
    return panel
  }

  override fun createSouthPanel(): JComponent {
    val southPanel = super.createSouthPanel()
    southPanel.layout = GridBagLayout()

    val gb = GridBagConstraints().apply {
      weightx = 1.0
      anchor = GridBagConstraints.CENTER
    }

    val buttonPanel = southPanel.getComponent(0)
    southPanel.remove(0)
    southPanel.add(buttonPanel, gb)

    return southPanel
  }

}