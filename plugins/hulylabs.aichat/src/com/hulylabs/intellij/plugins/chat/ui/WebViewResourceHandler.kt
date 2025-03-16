// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.ui.jcef.JBCefScrollbarsHelper
import org.cef.callback.CefCallback
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import java.awt.Color
import java.io.IOException
import java.io.InputStream
import javax.swing.UIManager
import kotlin.math.min

private const val STATIC_URL_PREFIX = "http://hulychat"
private val LOG = Logger.getInstance("#aichat.toolwindow")

class CustomResourceHandler : CefResourceHandler {
  private var myInputStream: InputStream? = null
  private var myMimeType: String? = null

  override fun processRequest(request: CefRequest?, callback: CefCallback?): Boolean {
    val url = request?.url
    if (url != null) {
      val ext = url.substringAfterLast(".", "txt")
      when (ext) {
        "html" -> myMimeType = "text/html"
        "js" -> myMimeType = "text/javascript"
        "css" -> myMimeType = "text/css"
        "map" -> myMimeType = "application/json"
        "ttf" -> myMimeType = "font/ttf"
        else -> myMimeType = "text/plain"
      }
      val path = url.replace(STATIC_URL_PREFIX, "/webview")
      if (path == "/webview/hulycode.css") {
        myInputStream = generateIdeCss().byteInputStream()
        callback?.Continue()
        return true
      }
      else {
        val resource = CustomResourceHandler::class.java.getResource(path)
        if (resource != null) {
          myInputStream = resource.openStream()
          callback?.Continue()
          return true
        }
      }
    }
    callback?.cancel()
    return false
  }

  override fun getResponseHeaders(response: CefResponse?, responseLength: IntRef?, redirectUrl: StringRef?) {
    response?.mimeType = myMimeType
    response?.status = 200
  }

  override fun readResponse(dataOut: ByteArray?, bytesToRead: Int, bytesRead: IntRef?, callback: CefCallback?): Boolean {
    try {
      val availableSize = myInputStream!!.available()
      if (availableSize > 0) {
        var bytesToRead = min(bytesToRead.toDouble(), availableSize.toDouble()).toInt()
        bytesToRead = myInputStream!!.read(dataOut, 0, bytesToRead)
        bytesRead?.set(bytesToRead)
        return true
      }
    }
    catch (e: IOException) {
      LOG.error(e)
    }
    bytesRead?.set(0)
    try {
      myInputStream!!.close()
    }
    catch (e: IOException) {
      LOG.error(e)
    }
    return false
  }

  override fun cancel() {
  }

  private fun Color.rgba(): String {
    val r = red
    val g = green
    val b = blue
    return String.format("#%02x%02x%02x%02x", r, g, b, alpha)
  }

  private fun generateIdeCss(): String {
    val css = StringBuilder()
    val themeToCssMap = mutableMapOf<String, String>(
      "TextPane.background" to "--bg-color",
      "TextPane.foreground" to "--text-color",
      "Button.default.focusColor" to "--component-focus-color",
      "Component.borderColor" to "--component-border-color",
    )
    val schemeToCssMap = mutableMapOf<String, String>(
      "DEFAULT_DOC_COMMENT" to "--prism-color-comment",
      "DEFAULT_COMMA" to "--prism-color-dot",
      "DEFAULT_IDENTIFIER" to "--prism-color-symbol",
      "DEFAULT_FUNCTION_CALL" to "--prism-color-function-call",
      "DEFAULT_STRING" to "--prism-color-string",
      "DEFAULT_OPERATION_SIGN" to "--prism-color-operator",
      "DEFAULT_KEYWORD" to "--prism-color-keyword",
      "DEFAULT_FUNCTION_DECLARATION" to "--prism-color-class-name",
      "DEFAULT_LOCAL_VARIABLE" to "--prism-color-variable",
      "DEFAULT_TAG" to "--prism-color-tag",
    )
    val font = UIManager.getFont("Panel.font")
    val scheme = EditorColorsManager.getInstance().activeVisibleScheme!!
    css.append(":root {\n")
    css.append("  --text-font-family: \"${font.family}\", -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Noto Sans\", \"Ubuntu Sans\", \"Liberation Sans\";\n")
    css.append("  --text-font-size: ${font.size}px;\n")
    css.append("  --inline-code-bg: rgba(127, 127, 127, 0.1);\n")
    css.append("  --text-color-rgb: ${scheme.defaultForeground.red}, ${scheme.defaultForeground.green}, ${scheme.defaultForeground.blue};\n")
    css.append("  --role-user-color: #4caf50;\n")
    css.append("  --role-assistant-color: #2196f3;\n")
    css.append("  --role-system-color: #ff9800;\n")
    for (themeKey in themeToCssMap.keys) {
      val cssKey = themeToCssMap[themeKey]
      css.append("  ${cssKey}: ${UIManager.getColor(themeKey).rgba()};\n")
    }
    css.append("  --prism-background: ${scheme.defaultBackground.rgba()};\n")
    css.append("  --prism-color: ${scheme.defaultForeground.rgba()};\n")
    css.append("  --prism-selection-background: ${scheme.getColor(ColorKey.createColorKey("SELECTION_BACKGROUND"))!!.rgba()};\n")
    for (schemeKey in schemeToCssMap.keys) {
      val cssKey = schemeToCssMap[schemeKey]
      val key = TextAttributesKey.find(schemeKey)
      val attributes = scheme.getAttributes(key, true)
      if (attributes != null && attributes.foregroundColor != null) {
        css.append("  ${cssKey}: ${attributes.foregroundColor.rgba()};\n")
      }
      else {
        css.append("  ${cssKey}: ${scheme.defaultForeground.rgba()};\n")
        LOG.warn("Can't find key $schemeKey")
      }
    }
    css.append(JBCefScrollbarsHelper.buildScrollbarsStyle())
    css.append("}\n")
    return css.toString()
  }
}