// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.onsave

import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import com.intellij.ide.actionsOnSave.ActionOnSaveInfo
import com.intellij.ide.actionsOnSave.ActionOnSaveInfoProvider
import com.intellij.openapi.components.service
import com.intellij.ui.components.DropDownLink

class PrettierOnSaveInfoProvider : ActionOnSaveInfoProvider() {
  override fun getActionOnSaveInfos(context: ActionOnSaveContext): Collection<ActionOnSaveInfo?> {
    return listOf(PrettierOnSaveActionInfo(context))
  }
}

class PrettierOnSaveActionInfo(context: ActionOnSaveContext)
  : BaseOnSaveActionInfo<BaseOnSaveSettingsState, PrettierOnSaveSettings>(
  context,
  BaseOnSaveSettingsState(),
  context.project.service(),
) {

  override fun getActionOnSaveName(): String = "Run Prettier"

  override fun getDropDownLinks(): List<DropDownLink<*>?> {
    return listOf(createFileTypesDropDownLink())
  }
}