// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.util.GradleConstants;

public final class GradleSourceSetData extends ModuleData {
  public static final @NotNull Key<GradleSourceSetData> KEY = Key.create(GradleSourceSetData.class, ProjectKeys.MODULE.getProcessingWeight() + 1);

  @PropertyMapping({"id", "externalName", "internalName", "moduleFileDirectoryPath", "externalConfigPath"})
  public GradleSourceSetData(
    @NotNull String id,
    @NotNull String externalName,
    @NotNull String internalName,
    @NotNull String moduleFileDirectoryPath,
    @NotNull String externalConfigPath
  ) {
    this(id, GradleProjectResolverUtil.getDefaultModuleTypeId(), externalName, internalName, moduleFileDirectoryPath, externalConfigPath);
  }

  @VisibleForTesting
  @ApiStatus.Internal
  public GradleSourceSetData(
    @NotNull String id,
    @NotNull String typeId,
    @NotNull String externalName,
    @NotNull String internalName,
    @NotNull String moduleFileDirectoryPath,
    @NotNull String externalConfigPath
  ) {
    super(id, GradleConstants.SYSTEM_ID, typeId, externalName, internalName, moduleFileDirectoryPath, externalConfigPath);
    setModuleName(getSourceSetName());
  }

  @Override
  public @NotNull String getIdeGrouping() {
    return super.getIdeGrouping() + ":" + getSourceSetName();
  }

  @Override
  public @Nullable String getIdeParentGrouping() {
    return super.getIdeGrouping();
  }

  private String getSourceSetName() {
    return StringUtil.substringAfterLast(getExternalName(), ":");
  }
}
