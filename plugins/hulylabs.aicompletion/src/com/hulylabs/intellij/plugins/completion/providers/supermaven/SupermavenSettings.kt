// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven

import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(name = "HulyCodeSupermaven", storages = [Storage("HulyCodeSupermaven.xml")])
class SupermavenSettings : SimplePersistentStateComponent<SettingsState>(SettingsState()) {
  companion object {
    @JvmStatic
    fun getInstance(): SupermavenSettings = service()
  }
}

class SettingsState : BaseState() {
  var agentVersion by property(8L)
  var agentDownloadUrl by string("")
  var agentVersionLastCheckTime by property(0L)
  var forceCheckVersion by property(0)
  var firstActivation by property(false)
  var gitignoreAllowed by property(false)
}
