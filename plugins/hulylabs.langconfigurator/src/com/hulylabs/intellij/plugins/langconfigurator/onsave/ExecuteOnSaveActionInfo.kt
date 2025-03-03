// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.onsave

import com.intellij.ide.actionsOnSave.ActionOnSaveComment
import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import com.intellij.ide.actionsOnSave.ActionOnSaveInfo
import com.intellij.ide.actionsOnSave.ActionOnSaveInfoProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.tools.Tool
import com.intellij.tools.ToolManager
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.DropDownLink

class ExecuteOnSaveInfoProvider : ActionOnSaveInfoProvider() {
  override fun getActionOnSaveInfos(context: ActionOnSaveContext): Collection<ActionOnSaveInfo?> {
    return listOf(ExecuteOnSaveActionInfo(context))
  }
}

class ExecuteOnSaveActionInfo(context: ActionOnSaveContext)
  : BaseOnSaveActionInfo<ExecuteOnSaveSettingsState, ExecuteOnSaveSettings>(
  context,
  ExecuteOnSaveSettingsState(),
  context.project.service(),
) {
  init {
    currentState.toolActionId = settings.state.toolActionId
  }

  override fun apply() {
    super.apply()
    settings.state.toolActionId = currentState.toolActionId
  }

  override fun isModified(): Boolean {
    return settings.state.toolActionId != currentState.toolActionId || super.isModified()
  }

  override fun getActionOnSaveName(): String = "Execute External Tool"

  override fun getComment(): ActionOnSaveComment? {
    val toolActionId = currentState.toolActionId
    if (toolActionId != null) {
      ToolManager.getInstance().tools.firstOrNull { it.actionId == toolActionId }?.let {
        return ActionOnSaveComment.info(it.name)
      }
    }
    return ActionOnSaveComment.info("No tool is selected")
  }

  override fun getActionLinks(): List<ActionLink?> {
    return listOf(createGoToPageInSettingsLink("preferences.externalTools"))
  }

  override fun getDropDownLinks(): List<DropDownLink<*>?> {
    return listOf(createSelectToolDropDownLink(), createFileTypesDropDownLink())
  }

  private fun createSelectToolDropDownLink(): DropDownLink<Tool?> {
    var link = DropDownLink<Tool?>(null) { selectCommandPopup(it) }
    link.text = "Select Tool"
    return link
  }

  private fun selectCommandPopup(link: DropDownLink<Tool?>): JBPopup {
    return JBPopupFactory.getInstance().createPopupChooserBuilder<Tool?>(ToolManager.getInstance().tools)
      .setRequestFocus(true)
      .setRenderer(SimpleListCellRenderer.create { label, tool, _ ->
        label.text = tool.name
      })
      .setItemChosenCallback { tool ->
        currentState.toolActionId = tool.actionId
      }
      .createPopup()
  }
}