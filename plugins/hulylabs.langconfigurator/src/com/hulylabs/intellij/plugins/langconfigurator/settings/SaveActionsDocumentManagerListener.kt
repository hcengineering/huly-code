// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.settings

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

class SaveActionsDocumentManagerListener(val project: Project) : FileDocumentManagerListener {
  private val psiDocumentManager = PsiDocumentManager.getInstance(project)

  override fun beforeAllDocumentsSaving() {
    if (!project.service<Settings>().state.formatOnSave) {
      return
    }
    val unsavedDocuments: List<Document> = listOf(*FileDocumentManager.getInstance().unsavedDocuments)
    if (unsavedDocuments.isNotEmpty()) {
      val files = unsavedDocuments.map { psiDocumentManager.getPsiFile(it)!! }.toTypedArray()
      val processor = ReformatCodeProcessor(project, files, null, false)
      processor.run()
    }
  }
}