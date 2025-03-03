// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.onsave

import com.intellij.openapi.components.*

@Service(Service.Level.PROJECT)
@State(name = "ExecuteOnSaveSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class ExecuteOnSaveSettings : SimplePersistentStateComponent<ExecuteOnSaveSettingsState>(ExecuteOnSaveSettingsState())

class ExecuteOnSaveSettingsState : BaseOnSaveSettingsState() {
  var toolActionId by string("")
}
