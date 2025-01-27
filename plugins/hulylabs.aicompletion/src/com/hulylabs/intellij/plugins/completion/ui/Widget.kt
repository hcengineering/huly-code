// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.ui

import com.hulylabs.intellij.plugins.completion.InlineCompletionProviderRegistry
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupFactory.ActionSelectionAid
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup

class Widget(project: Project) : EditorBasedStatusBarPopup(project, false) {
  override fun getWidgetState(file: VirtualFile?): WidgetState {
    val provider = InlineCompletionProviderRegistry.getInstance(project).provider
    val title = "${provider.name}: ${provider.getStatus()}"
    return WidgetState(title, title, true)
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
    val provider = InlineCompletionProviderRegistry.getInstance(project).provider
    actionGroup.addAll(provider.getActions(this.statusBar?.currentEditor?.invoke()?.file))
    return actionGroup
  }

}