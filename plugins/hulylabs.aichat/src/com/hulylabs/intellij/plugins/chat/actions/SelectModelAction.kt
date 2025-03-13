// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.actions

import com.hulylabs.intellij.plugins.chat.api.LanguageModel
import com.hulylabs.intellij.plugins.chat.providers.LanguageModelProviderRegistry
import com.hulylabs.intellij.plugins.chat.settings.ChatSettings
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import javax.swing.JComponent

class ModelItemAction(val model: LanguageModel)
  : AnAction("${model.provider.name}: ${model.displayName}") {
  override fun actionPerformed(e: AnActionEvent) {
    try {
      model.provider.loadModel(model)
      ChatSettings.getInstance().activeLanguageModel = model
    }
    catch (e: Exception) {
      e.printStackTrace()
    }
  }
}

class SelectModelAction(
  private val onSelected: () -> Unit
) : ComboBoxAction() {
  init {
    templatePresentation.description = "Select model"
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation

    var settings = ChatSettings.getInstance()
    if (settings.activeLanguageModel == null) {
      presentation.text = "No Model Selected"
    }
    else {
      presentation.text = "${settings.activeLanguageModel!!.provider.name}: ${settings.activeLanguageModel!!.displayName}"
    }
    presentation.isEnabledAndVisible = true
    onSelected()
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  override fun createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup {
    return object : DefaultActionGroup() {
      override fun getChildren(e: AnActionEvent?): Array<out AnAction?> {
        return LanguageModelProviderRegistry.getInstance()
          .getProviders()
          .filter { it.authenticated }
          .flatMap { it.providedModels() }
          .map { ModelItemAction(it)}
          .toTypedArray()
      }
    }
  }
}