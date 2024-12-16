// Copyright © 2024 HulyLabs. Use of this source code is governed by the Apache 2.0 license.
package hulylabs.hulycode.plugins.hulylangconfigurator.templates;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.system.CpuArch;
import com.redhat.devtools.lsp4ij.launching.ServerMappingSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HulyLanguageServerTemplate {
  private static final String DEFAULT_KEY = "default";
  private static final String OS_KEY = SystemInfo.isWindows ? "windows" : (SystemInfo.isMac ? "mac" : (SystemInfo.isUnix ? "unix" : ""));
  private static final String OS_ARCH_KEY = OS_KEY + "-" + (CpuArch.isArm64() ? "aarch64" : "x86_64");

  public String id;
  public String name;
  public String installCommand;
  public Map<String, String> programArgs;
  public Map<String, String> binaryUrls;
  public String settingsJson;
  public String initializationOptionsJson;
  public String clientSettingsJson;
  public List<HulyServerMappingSettings> mappingSettings;

  @NotNull
  public String getCommandLine() {
    return programArgs != null ? programArgs.getOrDefault(OS_KEY, programArgs.getOrDefault(DEFAULT_KEY, "")) : "";
  }

  @Nullable
  public String getBinaryUrl() {
    return binaryUrls != null ? binaryUrls.get(OS_ARCH_KEY) : null;
  }

  public List<ServerMappingSettings> getServerMappingSettings() {
    return mappingSettings.stream().map(HulyServerMappingSettings::toServerMappingSettings).filter(Objects::nonNull).toList();
  }
}
