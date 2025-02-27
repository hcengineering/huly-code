// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.scratch

import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.actions.KOTLIN_WORKSHEET_EXTENSION
import org.jetbrains.kotlin.idea.scratch.ui.KtScratchFileEditorWithPreview
import org.jetbrains.kotlin.idea.scratch.ui.findScratchFileEditorWithPreview
import org.jetbrains.kotlin.parsing.KotlinParserDefinition

internal val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.scratch")
internal fun Logger.printDebugMessage(str: String) {
    if (isDebugEnabled) debug("SCRATCH: $str")
}

val VirtualFile.isKotlinWorksheet: Boolean
    get() = name.endsWith(".$KOTLIN_WORKSHEET_EXTENSION")

val VirtualFile.isKotlinScratch: Boolean
    get() = KotlinParserDefinition.STD_SCRIPT_SUFFIX == this.extension &&
        ScratchFileService.getInstance().getRootType(this) is ScratchRootType

@TestOnly
fun getScratchEditorForSelectedFile(fileManager: FileEditorManager, virtualFile: VirtualFile): KtScratchFileEditorWithPreview? {
    val editor = fileManager.getSelectedEditor(virtualFile) as? TextEditor ?: return null
    return editor.findScratchFileEditorWithPreview()
}

fun TextEditor.getScratchFile(): ScratchFile? {
    return findScratchFileEditorWithPreview()?.scratchFile
}
