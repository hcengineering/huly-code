// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.onsave

import com.intellij.openapi.components.*

@Service(Service.Level.PROJECT)
@State(name = "PrettierOnSaveSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class PrettierOnSaveSettings : SimplePersistentStateComponent<BaseOnSaveSettingsState>(BaseOnSaveSettingsState())