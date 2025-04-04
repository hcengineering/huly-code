// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.profile.codeInspection;

import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolsSupplier;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public final class InspectionProfileLoadUtil {
  public static @NotNull String getProfileName(@NotNull Path file, @NotNull Element element) {
    String name = null;
    for (Element option : element.getChildren("option")) {
      if ("myName".equals(option.getAttributeValue("name"))) {
        name = option.getAttributeValue("value");
      }
    }
    if (name == null) {
      name = element.getAttributeValue("profile_name");
    }
    return name != null ? name : FileUtilRt.getNameWithoutExtension(file.getFileName().toString());
  }

  public static @NotNull InspectionProfileImpl load(@NotNull Path file,
                                                    @NotNull InspectionToolsSupplier registrar,
                                                    @NotNull InspectionProfileManager profileManager) throws JDOMException, IOException {
    Element element = JDOMUtil.load(file);
    String profileName = getProfileName(file, element);
    return load(element, profileName, registrar, profileManager);
  }

  public static @NotNull InspectionProfileImpl load(@NotNull Element element,
                                                    @NotNull String name,
                                                    @NotNull InspectionToolsSupplier registrar,
                                                    @NotNull InspectionProfileManager profileManager) throws InvalidDataException {
    InspectionProfileImpl profile = new InspectionProfileImpl(name, registrar, (BaseInspectionProfileManager)profileManager);
    final Element profileElement = element.getChild("profile");
    if (profileElement != null) {
      element = profileElement;
    }
    profile.readExternal(element);
    return profile;
  }
}
