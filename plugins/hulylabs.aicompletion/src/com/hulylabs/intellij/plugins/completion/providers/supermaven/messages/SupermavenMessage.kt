// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
@file:OptIn(ExperimentalSerializationApi::class)

package com.hulylabs.intellij.plugins.completion.providers.supermaven.messages

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement

@Serializable
@JsonClassDiscriminator("kind")
sealed class SupermavenMessage

@Serializable
@SerialName("response")
/** Main completion response message. */
data class SupermavenResponseMessage(
  val stateId: String,
  val items: List<ResponseItem>?,
) : SupermavenMessage()

@Serializable
data class ResponseItem(val kind: ResponseItemKind, val text: String? = null)

@Serializable
enum class ResponseItemKind {
  @SerialName("text")
  TEXT,

  @SerialName("delete")
  DELETE,

  @SerialName("dedent")
  DEDENT,

  @SerialName("end")
  END,

  @SerialName("barrier")
  BARRIER,

  @SerialName("skip")
  SKIP,

  @SerialName("jump")
  JUMP,

  @SerialName("finish_edit")
  FINISH_EDIT,
}

@Serializable
@SerialName("metadata")
data class SupermavenMetadataMessage(
  val dustStrings: List<String>,
) : SupermavenMessage()

@Serializable
@SerialName("connection_status")
data class SupermavenConnectionStatus(
  @SerialName("is_connected")
  val isConnected: Boolean,
  @SerialName("status_text")
  val statusText: String?,
) : SupermavenMessage()

@Serializable
@SerialName("user_status")
data class SupermavenUserStatus(
  val tier: String,
  val email: String?,
) : SupermavenMessage()

@Serializable
@SerialName("active_repo")
data class SupermavenActiveRepo(
  @SerialName("display_text")
  val displayText: String?,
  @SerialName("commit_hash")
  val commitHash: String? = null,
  @SerialName("repo_display_name")
  val repoDisplayName: String? = null,
  @SerialName("tooltip_text")
  val tooltipText: String? = null,
) : SupermavenMessage()

@Serializable
@SerialName("apology")
data class SupermavenApologyMessage(
  val message: String?,
) : SupermavenMessage()

@Serializable
@SerialName("activation_request")
data class SupermavenActivationRequestMessage(
  val activateUrl: String? = null,
) : SupermavenMessage()

@Serializable
@SerialName("activation_success")
data object SupermavenActivationSuccessMessage : SupermavenMessage()

@Serializable
@SerialName("passthrough")
data class SupermavenPassthroughMessage(
  val passthrough: SupermavenMessage,
) : SupermavenMessage()

@Serializable
enum class SupermavenPopupKind {
  @SerialName("open_url")
  OPEN_URL,

  @SerialName("no_op")
  NO_OP,
}

@Serializable
data class SupermavenPopupAction(
  val kind: SupermavenPopupKind,
  val label: String,
  val url: String,
)

@Serializable
@SerialName("popup")
data class SupermavenPopupMessage(
  val message: String,
  val actions: List<SupermavenPopupAction>,
) : SupermavenMessage()

@Serializable
enum class TaskStatusKind {
  @SerialName("in_progress")
  IN_PROGRESS,

  @SerialName("complete")
  COMPLETE,
}

@Serializable
@SerialName("task_status")
data class SupermavenTaskStatusMessage(
  val task: String,
  val percentComplete: Float?,
  val status: TaskStatusKind,
) : SupermavenMessage()

@Serializable
@SerialName("service_tier")
data class SupermavenServiceTierMessage(
  val serviceTier: String,
) : SupermavenMessage()

@Serializable
@SerialName("set")
data class SupermavenSetMessage(
  val key: String,
  val value: JsonElement,
) : SupermavenMessage()@Serializable

@SerialName("set_v2")
data class SupermavenSetV2Message(
  val key: String,
  val value: JsonElement,
) : SupermavenMessage()