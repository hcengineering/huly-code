// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.ui

import com.hulylabs.intellij.plugins.completion.CompletionProviderStateChangedListener
import com.hulylabs.intellij.plugins.completion.CompletionSettings
import com.hulylabs.intellij.plugins.completion.InlineCompletionProviderRegistry
import com.hulylabs.intellij.plugins.completion.actions.EnableAction
import com.hulylabs.intellij.plugins.completion.actions.FileEnableAction
import com.hulylabs.intellij.plugins.completion.actions.SwitchProviderAction
import com.hulylabs.intellij.plugins.completion.actions.ToggleDirectCallAction
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupFactory.ActionSelectionAid
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup

class Widget(project: Project) : EditorBasedStatusBarPopup(project, false) {
  init {
    project.messageBus.connect(this)
      .subscribe(CompletionProviderStateChangedListener.TOPIC,
                 object : CompletionProviderStateChangedListener {
                   override fun stateChanged() {
                     ApplicationManager.getApplication().invokeLater {
                       update()
                     }
                   }
                 })
  }

  override fun getWidgetState(file: VirtualFile?): WidgetState {
    if (CompletionSettings.getInstance().isCompletionEnabled()) {
      val provider = InlineCompletionProviderRegistry.getProvider(project)
      val title = "${provider.name}: ${provider.getStatus()}"
      return WidgetState(title, title, true)
    }
    else {
      val title = "Completion: disabled"
      return WidgetState(title, title, true)
    }
  }

  override fun createPopup(context: DataContext): ListPopup {
    return JBPopupFactory.getInstance().createActionGroupPopup(null, getActionGroup(), context, ActionSelectionAid.SPEEDSEARCH, true)
  }

  override fun createInstance(project: Project): StatusBarWidget {
    return Widget(project)
  }

  override fun ID(): String {
    return "AICompletionWidget"
  }

  private fun getActionGroup(): ActionGroup {
    val actionGroup = DefaultActionGroup()
    val provider = InlineCompletionProviderRegistry.getProvider(project)
    actionGroup.add(EnableAction(project))
    if (CompletionSettings.getInstance().isCompletionEnabled()) {
      actionGroup.add(ToggleDirectCallAction(project))
      actionGroup.add(SwitchProviderAction(project))
      actionGroup.add(Separator())
      val file = this.statusBar?.currentEditor?.invoke()?.file
      if (file != null && file.extension != null) {
        val extension = file.extension!!
        actionGroup.add(FileEnableAction(extension))
      }
      actionGroup.addAll(provider.getActions(file))
    }
    return actionGroup
  }

}