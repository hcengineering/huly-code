// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.messages;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class HulyLangConfiguratorBundle {
  private static final @NonNls String BUNDLE = "messages.HulyLangConfiguratorBundle";
  private static final DynamicBundle INSTANCE = new DynamicBundle(HulyLangConfiguratorBundle.class, BUNDLE);

  private HulyLangConfiguratorBundle() {
  }

  public static @NotNull @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
    return INSTANCE.getMessage(key, params);
  }
}
