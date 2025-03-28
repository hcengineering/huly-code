// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.testFramework.FileBasedTestCaseHelperEx;
import com.intellij.testFramework.Parameterized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @see LightQuickFixParameterizedTestCase5
 */
@RunWith(Parameterized.class)
public abstract class LightQuickFixParameterizedTestCase extends LightQuickFixTestCase implements FileBasedTestCaseHelperEx {
  @Override
  public @NotNull String getRelativeBasePath() {
    return getBasePath();
  }

  @Override
  public @Nullable String getFileSuffix(@NotNull String fileName) {
    if (!fileName.startsWith(BEFORE_PREFIX)) return null;
    return fileName.substring(BEFORE_PREFIX.length());
  }

  @Override
  public @Nullable String getBaseName(@NotNull String fileAfterSuffix) {
    if (!fileAfterSuffix.startsWith(AFTER_PREFIX)) return null;
    return fileAfterSuffix.substring(AFTER_PREFIX.length());
  }

  @Test
  public void runSingle() throws Throwable {
    doSingleTest(myFileSuffix, myTestDataPath);
  }
}
