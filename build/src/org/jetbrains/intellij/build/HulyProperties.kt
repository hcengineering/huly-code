// Copyright © 2024 Hardcore Engineering Inc. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import org.jetbrains.intellij.build.BuildPaths.Companion.COMMUNITY_ROOT
import org.jetbrains.intellij.build.impl.BuildContextImpl
import org.jetbrains.intellij.build.impl.PluginLayout.Companion.pluginAuto
import org.jetbrains.intellij.build.io.copyDir
import org.jetbrains.intellij.build.io.copyFileToDir
import org.jetbrains.intellij.build.kotlin.KotlinBinaries
import java.nio.file.Path

val HULY_BUNDLED_PLUGINS: PersistentList<String> = DEFAULT_BUNDLED_PLUGINS + sequenceOf(
  //JavaPluginLayout.MAIN_MODULE_NAME,
  //"intellij.java.ide.customization",
  //"intellij.copyright",
  "intellij.properties",
  "intellij.terminal",
  "intellij.emojipicker",
  "intellij.textmate",
  "intellij.editorconfig",
  //"intellij.settingsSync",
  "intellij.configurationScript",
  "intellij.json",
  "intellij.yaml",
  "intellij.html.tools",
  "intellij.tasks.core",
  //"intellij.repository.search",
  //"intellij.maven",
  //"intellij.maven.model",
  //"intellij.maven.server",
  //"intellij.gradle",
  //"intellij.gradle.dependencyUpdater",
  //"intellij.android.gradle.declarative.lang.ide",
  //"intellij.android.gradle.dsl",
  //"intellij.gradle.java",
  //"intellij.gradle.java.maven",
  //"intellij.gradle.analysis",
  "intellij.vcs.git",
  //"intellij.vcs.svn",
  //"intellij.vcs.hg",
  //"intellij.groovy",
  //"intellij.junit",
  //"intellij.testng",
  //"intellij.java.i18n",
  //"intellij.java.byteCodeViewer",
  //"intellij.java.coverage",
  //"intellij.java.decompiler",
  //"intellij.eclipse",
  "intellij.platform.langInjection",
  //"intellij.java.debugger.streams",
  "intellij.completionMlRanking",
  "intellij.completionMlRankingModels",
  //"intellij.statsCollector",
  "intellij.sh",
  "intellij.markdown",
  "intellij.webp",
  //"intellij.grazie",
  "intellij.featuresTrainer",
  "intellij.searchEverywhereMl",
  "intellij.marketplaceMl",
  //"intellij.platform.tracing.ide",
  "intellij.toml",
  //KotlinPluginBuilder.MAIN_KOTLIN_PLUGIN_MODULE,
  "intellij.keymap.eclipse",
  "intellij.keymap.visualStudio",
  "intellij.keymap.netbeans",
  "intellij.performanceTesting",
  "intellij.turboComplete",
  "redhat.lsp4ij",
  "hulylabs.langconfigurator",
  "hulylabs.treesitter",
  "hulylabs.aicompletion",
  "hulylabs.aichat",
)

internal suspend fun createHulyBuildContext(
  options: BuildOptions = BuildOptions(),
  projectHome: Path = COMMUNITY_ROOT.communityRoot,
): BuildContext {
  return BuildContextImpl.createContext(projectHome = projectHome,
                                        productProperties = HulyProperties(COMMUNITY_ROOT.communityRoot),
                                        setupTracer = true,
                                        proprietaryBuildTools = ProprietaryBuildTools(
                                          scrambleTool = null,
                                          signTool = MacOsSignTool(),
                                          macOsCodesignIdentity = MacOsCodesignIdentity(""),
                                          featureUsageStatisticsProperties = null,
                                          artifactsServer = null,
                                          licenseServerHost = null,
                                        ),
                                        options = options)
}

open class HulyProperties(private val communityHomeDir: Path) : BaseIdeaProperties() {
  companion object {
    val MAVEN_ARTIFACTS_ADDITIONAL_MODULES = persistentListOf(
      //"intellij.tools.jps.build.standalone",
      //"intellij.devkit.runtimeModuleRepository.jps",
      //"intellij.devkit.jps",
      "intellij.idea.community.build.tasks",
      "intellij.platform.debugger.testFramework",
      "intellij.platform.vcs.testFramework",
      "intellij.platform.externalSystem.testFramework",
      //"intellij.maven.testFramework",
      "intellij.tools.reproducibleBuilds.diff",
      //"intellij.space.java.jps",
    )
  }

  override val baseFileName: String
    get() = "huly-code"

  init {
    platformPrefix = "Huly"
    applicationInfoModule = "hulylabs.intellij.customization"
    scrambleMainJar = false
    useSplash = true
    buildCrossPlatformDistribution = true

    productLayout.productImplementationModules = listOf(
      "intellij.platform.starter",
      "hulylabs.intellij.customization",
    )
    productLayout.bundledPluginModules = HULY_BUNDLED_PLUGINS // + sequenceOf("intellij.vcs.github.community")

    productLayout.prepareCustomPluginRepositoryForPublishedPlugins = false
    productLayout.buildAllCompatiblePlugins = false
    productLayout.pluginLayouts = CommunityRepositoryModules.COMMUNITY_REPOSITORY_PLUGINS.addAll(listOf(
      //JavaPluginLayout.javaPlugin(),
      //CommunityRepositoryModules.androidPlugin(allPlatforms = true),
      //CommunityRepositoryModules.groovyPlugin(),
      pluginAuto("redhat.lsp4ij") { spec ->
        spec.withModuleLibrary("eclipse.lsp4j", "redhat.lsp4ij", "org.eclipse.lsp4j-0.21.1.jar")
        spec.withModuleLibrary("eclipse.lsp4j.debug", "redhat.lsp4ij", "org.eclipse.lsp4j.debug-0.21.1.jar")
        spec.withModuleLibrary("vladsch.flexmark", "redhat.lsp4ij", "flexmark-0.64.8.jar")
        spec.withModuleLibrary("nibor.autolink", "redhat.lsp4ij", "autolink-0.11.0.jar")
      },
      pluginAuto("hulylabs.langconfigurator") { spec ->
        spec.withModuleLibrary("tukaani.xz", "hulylabs.langconfigurator", "xz-1.10.jar")
        spec.withModuleLibrary("esotericsoftware.yamlbeans", "hulylabs.langconfigurator", "yamlbeans-1.17.jar")
      },
      pluginAuto("hulylabs.treesitter") { spec ->
        spec.withModuleLibrary("tree-sitter-libs", "hulylabs.treesitter", "tree-sitter-libs.jar")
      },
      pluginAuto("hulylabs.aicompletion") { spec ->
        spec.withModuleLibrary("eclipse.lsp4j.jsonrpc", "hulylabs.aicompletion", "eclipse.lsp4j.jsonrpc-0.23.1.jar")
      }
    ))

    productLayout.addPlatformSpec { layout, _ ->
      layout.withModule("intellij.platform.duplicates.analysis")
      layout.withModule("intellij.platform.structuralSearch")
    }

    mavenArtifacts.forIdeModules = true
    mavenArtifacts.additionalModules = mavenArtifacts.additionalModules.addAll(MAVEN_ARTIFACTS_ADDITIONAL_MODULES)
    mavenArtifacts.squashedModules = mavenArtifacts.squashedModules.addAll(persistentListOf(
      "intellij.platform.util.base",
      "intellij.platform.util.zip",
    ))

    //versionCheckerConfig = CE_CLASS_VERSIONS
    baseDownloadUrl = "https://dist.huly.io/code/"
    buildDocAuthoringAssets = true

    additionalVmOptions = persistentListOf(
      "-Dllm.show.ai.promotion.window.on.start=false",
      "-Djb.consents.confirmation.enabled=false",
      "-Didea.show.splash.longer=true"
    )
  }

  override suspend fun copyAdditionalFiles(context: BuildContext, targetDir: Path) {
    super.copyAdditionalFiles(context, targetDir)

    copyFileToDir(context.paths.communityHomeDir.resolve("LICENSE.txt"), targetDir)
    copyFileToDir(context.paths.communityHomeDir.resolve("NOTICE.txt"), targetDir)

    copyDir(
      sourceDir = context.paths.communityHomeDir.resolve("build/conf/ideaCE/common/bin"),
      targetDir = targetDir.resolve("bin"),
    )
    bundleExternalPlugins(context, targetDir)
  }

  protected open suspend fun bundleExternalPlugins(context: BuildContext, targetDirectory: Path) {
  }

  override fun createWindowsCustomizer(projectHome: String): WindowsDistributionCustomizer = HulyCodeWindowsDistributionCustomizer()
  override fun createLinuxCustomizer(projectHome: String): LinuxDistributionCustomizer = HulyCodeLinuxDistributionCustomizer()
  override fun createMacCustomizer(projectHome: String): MacDistributionCustomizer = HulyCodeMacDistributionCustomizer()

  protected open inner class HulyCodeWindowsDistributionCustomizer : WindowsDistributionCustomizer() {
    init {
      icoPath = "${communityHomeDir}/build/conf/hulycode/win/images/idea_CE.ico"
      icoPathForEAP = "${communityHomeDir}/build/conf/hulycode/win/images/idea_CE_EAP.ico"
      installerImagesPath = "${communityHomeDir}/build/conf/hulycode/win/images"
      fileAssociations = listOf("ts", "rs", "toml", "zig")
    }

    override fun getFullNameIncludingEdition(appInfo: ApplicationInfoProperties) = "Huly Code"

    override fun getFullNameIncludingEditionAndVendor(appInfo: ApplicationInfoProperties) = "Huly Code"

    override fun getUninstallFeedbackPageUrl(appInfo: ApplicationInfoProperties): String {
      return "" //"https://www.jetbrains.com/idea/uninstall/?edition=IC-${appInfo.majorVersion}.${appInfo.minorVersion}"
    }
  }

  protected open inner class HulyCodeLinuxDistributionCustomizer : LinuxDistributionCustomizer() {
    init {
      iconPngPath = "${communityHomeDir}/build/conf/hulycode/linux/images/icon_CE_128.png"
      iconPngPathForEAP = "${communityHomeDir}/build/conf/hulycode/linux/images/icon_CE_EAP_128.png"
      snapName = "huly-code"
      snapDescription =
        "A fast, minimal IDE for productive coding."
    }

    override fun getRootDirectoryName(appInfo: ApplicationInfoProperties, buildNumber: String) = "idea-IC-$buildNumber"

    override fun generateExecutableFilesPatterns(context: BuildContext, includeRuntime: Boolean, arch: JvmArchitecture): Sequence<String> {
      return super.generateExecutableFilesPatterns(context, includeRuntime, arch)
        .plus(KotlinBinaries.kotlinCompilerExecutables)
        .filterNot { it == "plugins/**/*.sh" }
    }
  }

  protected open inner class HulyCodeMacDistributionCustomizer : MacDistributionCustomizer() {
    init {
      icnsPath = "${communityHomeDir}/build/conf/hulycode/mac/images/idea.icns"
      icnsPathForEAP = "${communityHomeDir}/build/conf/hulycode/mac/images/communityEAP.icns"
      urlSchemes = listOf("huly-code")
      associateIpr = true
      fileAssociations = FileAssociation.from("ts", "rs", "toml", "zig")
      bundleIdentifier = "app.huly.HulyCode"
      dmgImagePath = "${communityHomeDir}/build/conf/hulycode/mac/images/dmg_background.png"
    }

    override fun getRootDirectoryName(appInfo: ApplicationInfoProperties, buildNumber: String): String {
      return if (appInfo.isEAP) {
        "Huly Code ${appInfo.majorVersion}.${appInfo.minorVersionMainPart}.app"
      }
      else {
        "Huly Code.app"
      }
    }

    override fun generateExecutableFilesPatterns(context: BuildContext, includeRuntime: Boolean, arch: JvmArchitecture): Sequence<String> {
      return super.generateExecutableFilesPatterns(context, includeRuntime, arch)
        .plus(KotlinBinaries.kotlinCompilerExecutables)
        .plus("jbr/Contents/Home/lib/jspawnhelper")
        .filterNot { it == "plugins/**/*.sh" }
    }
  }

  override fun getSystemSelector(appInfo: ApplicationInfoProperties, buildNumber: String): String {
    return "HulyCode${appInfo.majorVersion}.${appInfo.minorVersionMainPart}"
  }

  override fun getBaseArtifactName(appInfo: ApplicationInfoProperties, buildNumber: String) = "huly-code-$buildNumber"

  override fun getOutputDirectoryName(appInfo: ApplicationInfoProperties) = "huly-code"
}
