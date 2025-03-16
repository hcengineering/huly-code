// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.chat.providers.openrouter

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class OpenRouterModelPricing(
  val prompt: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class OpenRouterModel(
  val id: String,
  val name: String,
  val description: String,
  @JsonNames("context_length")
  val contextLength: Int,
  val pricing: OpenRouterModelPricing,
)