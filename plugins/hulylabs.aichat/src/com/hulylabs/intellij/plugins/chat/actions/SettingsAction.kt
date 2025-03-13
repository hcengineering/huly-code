// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ex.ConfigurableExtensionPointUtil
import com.intellij.openapi.options.ex.ConfigurableVisitor
import com.intellij.openapi.options.newEditor.SettingsDialogFactory

class SettingsAction : BaseChatAction("Settings", "config.svg") {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val groups = listOf(ConfigurableExtensionPointUtil.getConfigurableGroup(project, true))
    val configurable = ConfigurableVisitor.findById("preferences.aichat", groups)
    val dialog = SettingsDialogFactory.getInstance().create(project, groups, configurable, null)
    dialog.isModal = false
    dialog.show()
  }
}