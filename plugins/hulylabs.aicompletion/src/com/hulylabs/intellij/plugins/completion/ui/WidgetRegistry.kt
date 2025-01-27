// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.ui

import com.intellij.openapi.wm.StatusBarWidget

object WidgetRegistry {
  private val widgets = ArrayList<Widget>()

  fun registerWidget(widget: Widget): Widget {
    widgets.add(widget)
    return widget
  }

  fun unregisterWidget(widget: StatusBarWidget) {
    widgets.remove(widget)
  }
}