// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.settings

import com.intellij.openapi.components.*

@Service(Service.Level.PROJECT)
@State(name = "HulyCodeSettings", storages = [Storage("HulyCodeSettings.xml")])
class Settings: SimplePersistentStateComponent<SettingsState>(SettingsState())

class SettingsState : BaseState() {
  var formatOnSave by property(true)
}
