// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.updateSettings.impl.PatchInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.platform.ide.customization.ExternalProductResourceUrls
import com.intellij.util.Url
import com.intellij.util.Urls
import com.intellij.util.system.CpuArch

class HulyExternalResourceUrls : ExternalProductResourceUrls {
  private val baseUrl = "https://dist.huly.io/code/update/"
  override val updateMetadataUrl: Url
    get() = Urls.newFromEncoded(baseUrl).resolve("updates.xml")

  override val helpPageUrl: ((String) -> Url)?
    get() = { id -> Urls.newFromEncoded("https://github.com/hcengineering/huly-code") }

  override fun computePatchUrl(from: BuildNumber, to: BuildNumber): Url {
    val product = ApplicationInfo.getInstance().build.productCode
    val runtime = if (CpuArch.isArm64()) "-aarch64" else ""
    return Urls.newFromEncoded(baseUrl).resolve("${product}-${from.withoutProductCode().asString()}-${to.withoutProductCode().asString()}-patch${runtime}-${PatchInfo.OS_SUFFIX}.jar")
  }

  override val bugReportUrl: ((String) -> Url)
    get() = { _ -> Urls.newFromEncoded("https://github.com/hcengineering/huly-code/issues") }
}