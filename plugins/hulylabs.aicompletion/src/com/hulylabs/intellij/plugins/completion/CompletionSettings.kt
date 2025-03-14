// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion

import com.intellij.openapi.components.*
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.APP)
@State(name = "HulyCodeCompletion", storages = [Storage("HulyCodeCompletion.xml")])
class CompletionSettings : SimplePersistentStateComponent<SettingsState>(SettingsState()) {
  fun isCompletionEnabled(): Boolean {
    return state.completionEnabled
  }

  fun isCompletionEnabled(file: VirtualFile): Boolean {
    return state.completionEnabled && !state.disabledExtensions.contains(file.extension)
  }

  companion object {
    @JvmStatic
    fun getInstance(): CompletionSettings {
      return service()
    }
  }
}

class SettingsState : BaseState() {
  var onlyDirectCalls by property(false)
  var completionEnabled by property(true)
  var disabledExtensions by stringSet()
  var activeProviderIdx by property(0)
}
