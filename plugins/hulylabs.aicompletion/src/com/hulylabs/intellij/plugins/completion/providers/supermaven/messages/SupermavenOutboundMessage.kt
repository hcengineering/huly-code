// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven.messages

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

abstract class SupermavenOutboundMessage {
  abstract fun toJson(): String
}

interface StateUpdateMessage {
  fun toJson(): String
}

class SupermavenStateUpdateMessage(val newId: String, val updates: List<StateUpdateMessage>) : SupermavenOutboundMessage() {
  override fun toJson(): String {
    return """{"kind":"state_update","newId":"$newId","updates":[${updates.joinToString(",") { it.toJson() }}]}"""
  }
}

class FileUpdateMessage(val path: String, val content: String) : StateUpdateMessage {
  override fun toJson(): String {
    return """{"kind":"file_update","path":"$path","content":${Json.encodeToString(content)}}"""
  }
}

class CursorPositionUpdateMessage(val path: String, val offset: Int) : StateUpdateMessage {
  override fun toJson(): String {
    return """{"kind":"cursor_update","path":"$path","offset":$offset}"""
  }
}

class SupermavenLogoutMessage : SupermavenOutboundMessage() {
  override fun toJson(): String {
    return """{"kind":"logout"}"""
  }
}
