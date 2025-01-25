// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory

class WidgetFactory : StatusBarEditorBasedWidgetFactory() {
  override fun getId(): String {
    return "AIInlineCompletionWidget"
  }

  override fun getDisplayName(): String {
      return "AI Inline Completion Widget"
  }

  override fun createWidget(project: Project): StatusBarWidget {
    return WidgetRegistry.registerWidget(Widget(project))
  }

  override fun disposeWidget(widget: StatusBarWidget) {
    WidgetRegistry.unregisterWidget(widget)
  }
}