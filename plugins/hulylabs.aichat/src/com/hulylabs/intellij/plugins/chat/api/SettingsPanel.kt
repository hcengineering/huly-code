// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.api

import javax.swing.JComponent

interface SettingsPanel {
  fun isModified(): Boolean = false
  fun apply() {}
  fun reset() {}
  fun createComponent(): JComponent?
}