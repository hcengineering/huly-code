// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class TTLCache<K, V>(private val ttl: Duration = 5.minutes) {
  private data class CacheEntry<V>(val value: V, val timestamp: Long)

  private val cache = mutableMapOf<K, CacheEntry<V>>()

  fun put(key: K, value: V) {
    cleanup()
    cache[key] = CacheEntry(value, System.currentTimeMillis())
  }

  fun get(key: K): V? {
    cleanup()
    return cache[key]?.value
  }

  private fun cleanup() {
    val now = System.currentTimeMillis()
    cache.entries.removeIf {
      (now - it.value.timestamp) > ttl.inWholeMilliseconds
    }
  }
}