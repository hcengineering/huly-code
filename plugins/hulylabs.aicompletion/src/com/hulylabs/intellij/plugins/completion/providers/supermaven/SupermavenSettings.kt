// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(name = "HulyCodeSupermaven", storages = [Storage("HulyCodeSupermaven.xml")])
class SupermavenSettings: SimplePersistentStateComponent<SettingsState>(SettingsState())

class SettingsState : BaseState() {
  var firstActivation by property(false)
}
