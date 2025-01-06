// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class SettingsComponent {
  private val myFormatOnSave = JBCheckBox("Format on save")

  var panel: JPanel = FormBuilder.createFormBuilder()
    .addComponent(myFormatOnSave, 1)
    .addComponentFillVertically(JPanel(), 0)
    .panel
    private set

  var formatOnSave: Boolean
    get() = myFormatOnSave.isSelected
    set(value) {
      myFormatOnSave.isSelected = value
    }

  fun getPreferredFocusedComponent(): JComponent {
    return myFormatOnSave
  }
}