// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.onsave

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import kotlinx.serialization.json.*

val ESLINT_MESSAGES_KEY = Key.create<JsonArray>("ESLint.Messages")
class ESLintAnnotator : ExternalAnnotator<Boolean, Boolean>(), DumbAware {

  override fun collectInformation(file: PsiFile): Boolean? {
    return true
  }

  override fun doAnnotate(collectedInfo: Boolean?): Boolean? {
    return collectedInfo
  }

  override fun apply(file: PsiFile, annotationResult: Boolean?, holder: AnnotationHolder) {
    val messages = file.fileDocument.getUserData(ESLINT_MESSAGES_KEY)
    if (messages != null) {
      for (messageObj in messages) {
        val message = messageObj.jsonObject
        val startLine = message["line"]?.jsonPrimitive?.intOrNull
        val startColumn = message["column"]?.jsonPrimitive?.intOrNull
        val endLine = message["endLine"]?.jsonPrimitive?.intOrNull
        val endColumn = message["endColumn"]?.jsonPrimitive?.intOrNull
        val severity = message["severity"]?.jsonPrimitive?.intOrNull ?: 2
        val text = message["message"]?.jsonPrimitive?.contentOrNull
        val messageId = message["messageId"]?.jsonPrimitive?.contentOrNull?.let { "[$it]: " } ?: ""
        val ruleId = message["ruleId"]?.jsonPrimitive?.contentOrNull?.let { "ruleId: $it" } ?: ""
        if (text != null) {
          var builder = holder.newAnnotation(if (severity == 2) HighlightSeverity.ERROR else HighlightSeverity.WARNING, "$messageId$text")
          builder = builder
            .problemGroup { "ESLint" }
            .tooltip("$ruleId<br>$messageId$text")

          if (startLine != null && startColumn != null) {
            val startOffset = file.fileDocument.getLineStartOffset(startLine - 1) + startColumn - 1
            if (endLine != null && endColumn != null) {
              val endOffset = file.fileDocument.getLineStartOffset(endLine - 1) + endColumn - 1
              builder = builder.range(TextRange(startOffset, endOffset))
            } else {
              builder = builder.range(TextRange(startOffset, startOffset))
            }
          } else {
            builder = builder.fileLevel()
          }
          builder.create()
        }
      }
    }
  }
}