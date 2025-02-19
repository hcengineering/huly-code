// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.onsave

import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import com.intellij.ide.actionsOnSave.ActionOnSaveInfo
import com.intellij.ide.actionsOnSave.ActionOnSaveInfoProvider

class ExecuteOnSaveInfoProvider : ActionOnSaveInfoProvider() {
  override fun getActionOnSaveInfos(context: ActionOnSaveContext): Collection<ActionOnSaveInfo?> {
    return listOf(ExecuteOnSaveActionInfo(context))
  }
}