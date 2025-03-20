// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.supermaven.messages

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kind")
sealed class SupermavenOutboundMessage

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kind")
sealed class StateUpdateMessage

@Serializable
@SerialName("inform_file_changed")
data class SupermavenInformFileChangedMessage(
  val path: String,
) : SupermavenOutboundMessage()

@Serializable
@SerialName("state_update")
data class SupermavenStateUpdateMessage(
  val newId: String,
  val updates: List<StateUpdateMessage>,
) : SupermavenOutboundMessage()

@Serializable
@SerialName("file_update")
data class FileUpdateMessage(
  val path: String,
  val content: String,
) : StateUpdateMessage()

@Serializable
@SerialName("cursor_update")
data class CursorPositionUpdateMessage(
  val path: String,
  val offset: Int,
) : StateUpdateMessage()

@Serializable
@SerialName("logout")
data object SupermavenLogoutMessage : SupermavenOutboundMessage()

@Serializable
@SerialName("use_free_version")
data class SupermavenFreeActivationMessage(
  val userEmail: String? = null,
) : SupermavenOutboundMessage()

@Serializable
@SerialName("greeting")
data class SupermavenGreetingsMessage(
  val allowGitignore: Boolean,
) : SupermavenOutboundMessage()