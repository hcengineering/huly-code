// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.fileTypes;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.nio.charset.StandardCharsets;

public class RustFileType implements FileType {
  public static final FileType INSTANCE = new RustFileType();

  private RustFileType() {
  }

  @Override
  public @NotNull String getName() {
    return "Rust";
  }

  @Override
  public @NotNull String getDescription() {
    return "Rust";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "rs";
  }

  @Override
  public Icon getIcon() {
    return IconManager.getInstance().getIcon("icons/rust.svg", RustFileType.class.getClassLoader());
  }

  @Override
  public boolean isBinary() {
    return false;
  }

  @Override
  public String getCharset(@NotNull VirtualFile file, byte @NotNull [] content) {
    return StandardCharsets.UTF_8.name();
  }
}
