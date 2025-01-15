// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.terminal

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

class FileLinkTerminalFilterProvider : ConsoleFilterProvider {
  override fun getDefaultFilters(project: Project): Array<Filter> {
    return arrayOf(FileLinkTerminalFilter(project))
  }
}

class FileLinkTerminalFilter(val project: Project) : Filter {
  private val FILE_PATTERN = Regex("""([\\|/]?([\w|.]+[\\|/])+[\w|.]+\.\w+)(:(\d+):(\d+))?""")

  override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
    val matchResult = FILE_PATTERN.find(line)
    if (matchResult != null) {
      val filePath = matchResult.groupValues[1]
      var lineNumber = -1
      var columnNumber = -1
      if (matchResult.groupValues[4].isNotEmpty()) {
        lineNumber = matchResult.groupValues[4].toInt()
      }
      if (matchResult.groupValues[5].isNotEmpty()) {
        columnNumber = matchResult.groupValues[5].toInt()
      }
      val file = project.guessProjectDir()?.findFileByRelativePath(filePath.replace("\\", "/"))
      if (file != null) {
        val startOffset = entireLength - (line.length - matchResult.range.first)
        return Filter.Result(startOffset, startOffset + matchResult.groupValues[0].length) { project ->
          ApplicationManager.getApplication().runReadAction {
            val fileDescriptor = if (lineNumber != -1) OpenFileDescriptor(project, file, lineNumber, columnNumber) else OpenFileDescriptor(project, file)
            FileEditorManager.getInstance(project).openFileEditor(fileDescriptor, true)
          }
        }
      }
    }
    return null
  }
}
