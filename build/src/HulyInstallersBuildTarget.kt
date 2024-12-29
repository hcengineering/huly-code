// Copyright © 2024 Hardcore Engineering Inc. Use of this source code is governed by the Apache 2.0 license.
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.intellij.build.BuildOptions
import org.jetbrains.intellij.build.CompilationTasks
import org.jetbrains.intellij.build.createHulyBuildContext
import org.jetbrains.intellij.build.impl.buildDistributions

internal object HulyInstallersBuildTarget {
  @JvmStatic
  fun main(args: Array<String>) {
    val options = BuildOptions().apply {
      incrementalCompilation = true
      useCompiledClassesFromProjectOutput = false
    }

    runBlocking(Dispatchers.Default) {
      val context = createHulyBuildContext(options)
      CompilationTasks.create(context).compileModules(moduleNames = null)
      buildDistributions(context)
    }
  }
}