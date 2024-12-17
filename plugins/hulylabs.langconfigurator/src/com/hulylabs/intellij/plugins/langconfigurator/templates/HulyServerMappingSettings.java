// Copyright © 2024 HulyLabs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.templates;

import com.redhat.devtools.lsp4ij.launching.ServerMappingSettings;
import org.jetbrains.annotations.Nullable;

public class HulyServerMappingSettings {
  public String language;
  public String languageId;
  public HulyServerMappingFileType fileType;

  @Nullable
  public ServerMappingSettings toServerMappingSettings() {
    if (language != null) {
      return ServerMappingSettings.createLanguageMappingSettings(language, languageId);
    } else if (fileType != null && fileType.name != null && fileType.patterns == null) {
      return ServerMappingSettings.createFileTypeMappingSettings(fileType.name, languageId);
    } else if (fileType != null && fileType.patterns != null) {
      return ServerMappingSettings.createFileNamePatternsMappingSettings(fileType.patterns, languageId);
    } else {
      return null;
    }
  }
}
