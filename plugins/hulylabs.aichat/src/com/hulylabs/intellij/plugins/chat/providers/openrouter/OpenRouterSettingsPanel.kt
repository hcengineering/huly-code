// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.providers.openrouter

import com.hulylabs.intellij.plugins.chat.api.SettingsPanel
import com.hulylabs.intellij.plugins.chat.settings.ChatSettings
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.icons.AllIcons
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.EDT
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CheckBoxList
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBHtmlPane
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.*
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import javax.swing.*

class OpenRouterSettingsPanel(val provider: OpenRouterProvider) : JPanel(BorderLayout()), SettingsPanel {
  private val description: String = """
    To use OpenRouter in Huly Code Chat, you need to add API key. Follow the steps below:<br>
    &nbsp; - Go to <a href="https://openrouter.ai/settings/keys">https://openrouter.ai/settings/keys</a><br>
    &nbsp; - Click <code>Create Key</code><br>
    &nbsp; - Copy the API key and paste it in the field below
  """

  private var storedApiKey: String? = null
  private val scope = MainScope().plus(CoroutineName("OpenRouter"))

  private val apiKeyField = JBPasswordField().apply {
    emptyText.text = "Enter OpenRouter API Key"
  }

  private val searchField = SearchTextField(false, true, "Search models...")

  private val modelsList = object : CheckBoxList<OpenRouterModel>() {
    init {
      setEmptyText("No models loaded")
    }

    override fun adjustRendering(rootComponent: JComponent?, checkBox: JCheckBox?, index: Int, selected: Boolean, hasFocus: Boolean): JComponent? {
      var item = getItemAt(index)
      val price = item?.pricing?.prompt?.toDouble()?.let { it * 1_000_000 } ?: 0.0
      val priceString = if (price == 0.0) "free" else "$${String.format("%.2f", price)}"
      val priceLabel: JBLabel = JBLabel(priceString, SwingConstants.RIGHT).apply {
        preferredSize = JBUI.size(100, preferredSize.height)
        border = JBUI.Borders.emptyRight(checkBox?.getInsets()?.left ?: 0)
        font = UIUtil.getFont(UIUtil.FontSize.SMALL, font)
        foreground = if (selected) getForeground(true) else JBColor.GRAY
        background = getBackground(selected)
      }
      if (item?.description != null) {
        rootComponent?.toolTipText = """
        <html>
        <b>${item.name}</b><br>
        Context size: ${item.contextLength} tokens<br>
        Price: ${if (price == 0.0) "free" else "$${String.format("%.2f", price)} per million tokens"}<br><br>
        ${item.description}<br>
        </html>
        """
      }
      rootComponent?.add(priceLabel, BorderLayout.LINE_END)
      return rootComponent
    }

    override fun getSecondaryText(index: Int): @Nls String? {
      return getItemAt(index)?.contextLength?.toString()
    }
  }
  private val availableModels = mutableListOf<OpenRouterModel>()

  private val refreshModelsButton = JButton("Refresh", AllIcons.Actions.Refresh).apply {
    addActionListener {
      refreshModels(true)
    }
  }

  init {
    val descriptionLabel = JBHtmlPane().apply {
      foreground = JBColor.getColor("Label.infoForeground")
      background = JBColor.PanelBackground
      text = description
    }

    val modelsPanel = JPanel(BorderLayout()).apply {
      border = JBUI.Borders.empty()

      add(JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty()
        add(searchField, BorderLayout.CENTER)
        add(refreshModelsButton, BorderLayout.EAST)
      }, BorderLayout.NORTH)

      add(JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(0, 3)
        add(JBScrollPane(modelsList).apply {
          preferredSize = JBUI.size(300, 200)
          border = JBUI.Borders.customLine(JBColor.getColor("Panel.border"))
        }, BorderLayout.CENTER)
      }, BorderLayout.CENTER)
    }

    val formBuilder = FormBuilder.createFormBuilder()
      .addComponent(descriptionLabel)
      .addLabeledComponent("API key:", apiKeyField)
      .addLabeledComponent("Models:", modelsPanel, true)
    add(formBuilder.panel, BorderLayout.CENTER)

    searchField.addDocumentListener(object : javax.swing.event.DocumentListener {
      override fun insertUpdate(e: javax.swing.event.DocumentEvent) = filterModels()
      override fun removeUpdate(e: javax.swing.event.DocumentEvent) = filterModels()
      override fun changedUpdate(e: javax.swing.event.DocumentEvent) = filterModels()
    })
    refreshModels(false)
  }

  private fun updateModels(models: List<OpenRouterModel>) {
    availableModels.clear()
    availableModels.addAll(models)
    modelsList.clear()
    val models = ChatSettings.getInstance().state.openRoutersModels
    for (model in availableModels) {
      modelsList.addItem(model, model.name, models.contains(model.id))
    }
  }

  private fun filterModels() {
    val searchText = searchField.text.lowercase()
    val filteredModels = availableModels.filter { it -> it.name.lowercase().contains(searchText) }
    modelsList.clear()
    for (model in filteredModels) {
      modelsList.addItem(model, model.name, false)
    }
  }

  private fun refreshModels(force: Boolean = false) {
    refreshModelsButton.isEnabled = false

    scope.launch {
      try {
        val service = OpenRouterService.getInstance()
        service.loadModels(force)
        withContext(Dispatchers.EDT) {
          updateModels(service.getModels())
        }
      }
      catch (e: Exception) {
        withContext(Dispatchers.EDT) {
          Messages.showErrorDialog(
            "Failed to refresh models: ${e.message}",
            "Refresh Models"
          )
        }
      }
      finally {
        withContext(Dispatchers.EDT) {
          refreshModelsButton.isEnabled = true
        }
      }
    }
  }

  override fun createComponent(): JComponent = this

  override fun isModified(): Boolean {
    val currentApiKey = String(apiKeyField.password)
    if (currentApiKey.isEmpty() && storedApiKey.isNullOrEmpty()) {
      return false
    }
    val models = ChatSettings.getInstance().state.openRoutersModels
    val selectedModels = modelsList.checkedItems.map { it.id }.toSet()
    return currentApiKey != storedApiKey || selectedModels != models
  }

  override fun apply() {
    saveApiKey(String(apiKeyField.password))
    val selectedModels = modelsList.checkedItems.map { it.id }.toSet()
    ChatSettings.getInstance().state.openRoutersModels.clear()
    ChatSettings.getInstance().state.openRoutersModels.addAll(selectedModels)
  }

  override fun reset() {
    scope.launch {
      getStoredApiKey()
      withContext(Dispatchers.EDT) {
        apiKeyField.text = storedApiKey
        modelsList.clear()
        val models = ChatSettings.getInstance().state.openRoutersModels
        for (model in availableModels) {
          modelsList.addItem(model, model.name, models.contains(model.id))
        }
      }
    }
  }

  private fun getStoredApiKey(): String? {
    if (storedApiKey == null) {
      val passwordSafe = PasswordSafe.Companion.instance
      val attributes = CredentialAttributes(
        generateServiceName("HulyChat", "OpenRouterApiKey")
      )
      storedApiKey = passwordSafe.getPassword(attributes)
    }
    return storedApiKey
  }

  private fun saveApiKey(key: String?) {
    storedApiKey = key
    val passwordSafe = PasswordSafe.Companion.instance
    val attributes = CredentialAttributes(
      generateServiceName("HulyChat", "OpenRouterApiKey")
    )
    passwordSafe.setPassword(attributes, key)
    OpenRouterService.getInstance().updateApiKey(key)
  }
}