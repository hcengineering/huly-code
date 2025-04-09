// Copyright © 2024 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.templates;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service(Service.Level.APP)
public final class HulyLanguageServerTemplateManager {
  private static final Logger LOG = Logger.getInstance(HulyLanguageServerTemplateManager.class);
  private static final String TEMPLATES_DIR = "lsp-templates";

  private final Map<String, HulyLanguageServerTemplate> templatesByExt = new HashMap<>();
  private final Map<String, HulyLanguageServerTemplate> templatesByLangName = new HashMap<>();
  private final Map<String, HulyLanguageServerTemplate> templatesById = new HashMap<>();

  private HulyLanguageServerTemplateManager() {
    VirtualFile templateRoot = getTemplateRoot();
    if (templateRoot != null) {
      for (VirtualFile templateDir : templateRoot.getChildren()) {
        if (templateDir.isDirectory()) {
          try {
            HulyLanguageServerTemplate template = importTemplate(templateDir);
            if (template != null) {
              templatesById.put(template.id, template);
              template.mappingSettings.forEach(mapping -> {
                if (mapping.language != null) {
                  templatesByLangName.put(mapping.language, template);
                }
                if (mapping.fileType != null) {
                  if (mapping.fileType.patterns != null) {
                    mapping.fileType.patterns.forEach(
                      pattern -> templatesByExt.put(pattern.replaceFirst("\\*\\.", "").toLowerCase(Locale.ROOT), template));
                  }
                  else if (mapping.fileType.name != null) {
                    templatesByLangName.put(mapping.fileType.name, template);
                  }
                }
              });
            }
            else {
              LOG.warn(String.format("No template found in %s", templateDir));
            }
          }
          catch (IOException ex) {
            LOG.warn(ex.getLocalizedMessage(), ex);
          }
        }
      }
    }
    else {
      LOG.warn("No templateRoot found, no templates");
    }
  }

  @Nullable
  public VirtualFile getTemplateRoot() {
    URL url = HulyLanguageServerTemplateManager.class.getClassLoader().getResource(TEMPLATES_DIR);
    if (url == null) {
      LOG.warn("No " + TEMPLATES_DIR + " directory/url found");
      return null;
    }
    try {
      // url looks like jar:file:/Users/username/Library/Application%20Support/<IDE>/plugins/<plugin>/lib/<plugin>.jar!/data/lsp-templates
      String filePart = url.toURI().getRawSchemeSpecificPart(); // get un-decoded, URI compatible part
      LOG.debug("Templates filePart : {}", filePart);
      String resourcePath = new URI(filePart).getSchemeSpecificPart();
      LOG.debug("Templates resources path from uri : {}", resourcePath);
      return VfsUtil.findFileByURL(url);
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return null;
  }

  @Nullable
  public HulyLanguageServerTemplate getTemplate(@NotNull VirtualFile file) {
    String ext = file.getExtension();
    String langName = file.getFileType().getName();
    return templatesByLangName.getOrDefault(langName, ext != null ? templatesByExt.get(ext.toLowerCase(Locale.ROOT)) : null);
  }

  @Nullable
  public HulyLanguageServerTemplate findTemplateByName(@NotNull String name) {
    for (HulyLanguageServerTemplate template : templatesById.values()) {
      if (template.name.equals(name)) {
        return template;
      }
    }
    return null;
  }

  @Nullable
  public HulyLanguageServerTemplate getTemplateById(@NotNull String id) {
    return templatesById.get(id);
  }


  @Nullable
  private static HulyLanguageServerTemplate importTemplate(@NotNull VirtualFile templateFolder) throws IOException {
    VirtualFile templateFile = templateFolder.findChild("template.yaml");
    if (templateFile == null) return null;
    HulyLanguageServerTemplate template;
    try (InputStream stream = templateFile.getInputStream()) {
      YamlReader reader = new YamlReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
      template = reader.read(HulyLanguageServerTemplate.class);
    }
    VirtualFile clientSettingsFile = templateFolder.findChild("clientSettings.json");
    if (clientSettingsFile != null) {
      try (InputStream stream = clientSettingsFile.getInputStream()) {
        template.clientSettingsJson = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      }
    }
    VirtualFile initializationOptionsFile = templateFolder.findChild("initializationOptions.json");
    if (initializationOptionsFile != null) {
      try (InputStream stream = initializationOptionsFile.getInputStream()) {
        template.initializationOptionsJson = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      }
    }
    VirtualFile settingsFile = templateFolder.findChild("settings.json");
    if (settingsFile != null) {
      try (InputStream stream = settingsFile.getInputStream()) {
        template.settingsJson = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      }
    }
    return template;
  }
}
