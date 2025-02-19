// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.onsave

import com.hulylabs.intellij.plugins.langconfigurator.LSPFileTypesRegistry
import com.intellij.ide.actionsOnSave.ActionOnSaveComment
import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import com.intellij.ide.actionsOnSave.ActionOnSaveInfo
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.NlsContexts
import com.intellij.tools.Tool
import com.intellij.tools.ToolManager
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTree.CheckboxTreeCellRenderer
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.DropDownLink
import com.intellij.ui.components.JBScrollPane
import java.util.*
import java.util.function.Function
import javax.swing.JTree
import javax.swing.tree.TreePath

class ExecuteOnSaveActionInfo(context: ActionOnSaveContext) : ActionOnSaveInfo(context) {
  var settings: ExecuteOnSaveSettings = project.service()
  var currentState: ExecuteOnSaveSettingsState = ExecuteOnSaveSettingsState()

  init {
    currentState.isEnabled = settings.state.isEnabled
    currentState.toolActionId = settings.state.toolActionId
    currentState.languageIds = settings.state.languageIds
    currentState.allLanguageIdsSelected = settings.state.allLanguageIdsSelected
  }

  override fun apply() {
    settings.state.toolActionId = currentState.toolActionId
    settings.state.languageIds = currentState.languageIds
    settings.state.isEnabled = currentState.isEnabled
    settings.state.allLanguageIdsSelected = currentState.allLanguageIdsSelected
  }

  override fun isModified(): Boolean {
    return settings.state.isEnabled != currentState.isEnabled
           || settings.state.toolActionId != currentState.toolActionId
           || settings.state.languageIds != currentState.languageIds
           || settings.state.allLanguageIdsSelected != currentState.allLanguageIdsSelected
  }

  override fun getActionOnSaveName(): String = "Execute External Tool"

  override fun getComment(): ActionOnSaveComment? {
    val toolActionId = currentState.toolActionId
    if (toolActionId != null) {
      ToolManager.getInstance().tools.firstOrNull { it.actionId == toolActionId }?.let {
        return ActionOnSaveComment.info(it.name)
      }
    }
    return ActionOnSaveComment.info("No tool is selected")
  }

  override fun isActionOnSaveEnabled(): Boolean {
    return currentState.isEnabled
  }

  override fun setActionOnSaveEnabled(enabled: Boolean) {
    currentState.isEnabled = enabled
  }

  override fun getActionLinks(): List<ActionLink?> {
    return listOf(createGoToPageInSettingsLink("preferences.externalTools"))
  }

  override fun getDropDownLinks(): List<DropDownLink<*>?> {
    return listOf(createSelectToolDropDownLink(), createFileTypesDropDownLink())
  }

  private fun createSelectToolDropDownLink(): DropDownLink<Tool?> {
    var link = DropDownLink<Tool?>(null) { selectCommandPopup(it) }
    link.text = "Select Tool"
    return link
  }

  private fun createFileTypesDropDownLink(): DropDownLink<String?> {
    return DropDownLink(getFileTypesLinkText()) { createFileTypesPopup(it) }
  }

  private fun selectCommandPopup(link: DropDownLink<Tool?>): JBPopup {
    return JBPopupFactory.getInstance().createPopupChooserBuilder<Tool?>(ToolManager.getInstance().tools)
      .setRequestFocus(true)
      .setRenderer(SimpleListCellRenderer.create { label, tool, _ ->
        label.text = tool.name
      })
      .setItemChosenCallback { tool ->
        currentState.toolActionId = tool.actionId
      }
      .createPopup()
  }

  private fun getFileTypesLinkText(): @NlsContexts.LinkLabel String {
    if (currentState.allLanguageIdsSelected) {
      return "All files types"
    }
    val languageIds: MutableSet<String> = currentState.languageIds

    if (languageIds.isEmpty()) {
      return "Select files types"
    }

    val languageId = languageIds.iterator().next()
    if (languageIds.size == 1) {
      return "Files: $languageId"
    }

    return "Files: $languageId, ${languageIds.size - 1} more"
  }

  private fun createFileTypesPopup(link: DropDownLink<String?>): JBPopup {
    val root = CheckedTreeNode("All file types")

    LSPFileTypesRegistry.instance.getLanguageIds().sorted().forEach {
      root.add(CheckedTreeNode(it))
    }

    val result: SortedSet<FileType?> = TreeSet<FileType?>(Comparator.comparing<FileType?, String?>(Function { obj: FileType? -> obj!!.getDescription() }, java.lang.String.CASE_INSENSITIVE_ORDER))

    val fileTypeManager = FileTypeManager.getInstance()
    for (fileType in fileTypeManager.getRegisteredFileTypes()) {
      if (fileType is LanguageFileType && !fileTypeManager.getAssociations(fileType).isEmpty()) {
        result.add(fileType)
      }
    }

    for (fileType in result) {
      root.add(CheckedTreeNode(fileType))
    }

    val tree: CheckboxTree = createFileTypesCheckboxTree(root)

    return JBPopupFactory.getInstance().createComponentPopupBuilder(JBScrollPane(tree), tree)
      .setRequestFocus(true)
      .addListener(object : JBPopupListener {
        override fun onClosed(event: LightweightWindowEvent) {
          onFileTypePopupClosed(link, root)
        }
      })
      .createPopup()
  }

  private fun createFileTypesCheckboxTree(root: CheckedTreeNode): CheckboxTree {
    val tree: CheckboxTree = object : CheckboxTree(createFileTypesRenderer(), root) {
      override fun installSpeedSearch() {
        TreeSpeedSearch.installOn(this, false, Function { path: TreePath? ->
          val node = path!!.lastPathComponent as CheckedTreeNode
          val userObject = node.getUserObject()
          if (userObject is FileType) {
            return@Function userObject.getDescription()
          }
          userObject.toString()
        })
      }
    }

    tree.setRootVisible(true)
    tree.setSelectionRow(0)

    resetTree(root)

    return tree
  }

  private fun resetTree(root: CheckedTreeNode) {
    if (currentState.allLanguageIdsSelected) {
      root.setChecked(true)
      return
    }

    root.setChecked(false)

    val fileTypesEnum = root.children()
    while (fileTypesEnum.hasMoreElements()) {
      val node = fileTypesEnum.nextElement() as CheckedTreeNode
      val userObject = node.getUserObject()
      when (userObject) {
        is String -> node.setChecked(currentState.languageIds.contains(userObject))
        is LanguageFileType -> node.setChecked(currentState.languageIds.contains(userObject.language.id))
      }
    }
  }

  private fun createFileTypesRenderer(): CheckboxTreeCellRenderer {
    return object : CheckboxTreeCellRenderer() {
      override fun customizeRenderer(t: JTree?, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, focus: Boolean) {
        if (value !is CheckedTreeNode) return

        val userObject = value.getUserObject()

        if (userObject is String) {
          textRenderer.append(userObject)
        }
        if (userObject is LanguageFileType) {
          textRenderer.setIcon(userObject.icon)
          textRenderer.append(userObject.description)
        }
      }
    }
  }

  private fun onFileTypePopupClosed(link: DropDownLink<String?>, root: CheckedTreeNode) {
    currentState.languageIds.clear()
    if (root.isChecked) {
      currentState.allLanguageIdsSelected = true
    } else {
      currentState.allLanguageIdsSelected = false
      val fileTypesEnum = root.children()
      while (fileTypesEnum.hasMoreElements()) {
        val node = fileTypesEnum.nextElement() as CheckedTreeNode
        if (node.isChecked()) {
          val userObject = node.getUserObject()
          when (userObject) {
            is String -> currentState.languageIds.add(userObject)
            is LanguageFileType -> currentState.languageIds.add(userObject.language.id)
          }
        }
      }
    }
    link.setText(getFileTypesLinkText())
  }
}