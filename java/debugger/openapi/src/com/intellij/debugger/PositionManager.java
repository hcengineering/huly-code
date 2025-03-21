// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.debugger;

import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.util.concurrency.annotations.RequiresBlockingContext;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Set;

/**
 * Manages the correspondence between source positions and bytecode locations during JVM debugging.
 * Instances of this class are created by the factory registered via the {@link PositionManagerFactory} extension point.
 *
 * @see com.intellij.debugger.engine.JSR45PositionManager
 */
public interface PositionManager {
  /**
   * Returns the source position corresponding to the specified bytecode location.
   *
   * @param location the bytecode location.
   * @return the corresponding source position.
   * @throws NoDataException if the location is not in the code managed by this {@code PositionManager}
   */
  @Nullable
  @RequiresBlockingContext
  SourcePosition getSourcePosition(@Nullable Location location) throws NoDataException;

  /**
   * Returns the list of all Java classes corresponding to the specified position in the source code.
   *
   * @param classPosition the source position.
   * @return the list of corresponding Java classes.
   * @throws NoDataException if the location is not in the code managed by this {@code PositionManager}
   * @see com.intellij.debugger.engine.jdi.VirtualMachineProxy#classesByName
   */
  @NotNull
  @Unmodifiable
  List<ReferenceType> getAllClasses(@NotNull SourcePosition classPosition) throws NoDataException;

  /**
   * Returns the list of bytecode locations in a specific class corresponding to the specified position in the source code.
   *
   * @param type     a Java class (one of the list returned by {@link #getAllClasses}).
   * @param position the position in the source code.
   * @return the list of corresponding bytecode locations.
   * @throws NoDataException if the location is not in the code managed by this {@code PositionManager}
   * @see ReferenceType#locationsOfLine(int)
   */
  @NotNull
  List<Location> locationsOfLine(@NotNull ReferenceType type, @NotNull SourcePosition position) throws NoDataException;

  /**
   * Called to request the JVM to notify the debugger engine when a class corresponding to a breakpoint location is loaded.
   * The implementation should calculate the pattern of the class files corresponding to the breakpoint location and call
   * {@link com.intellij.debugger.requests.RequestManager#createClassPrepareRequest} to create the request.
   *
   * @param requestor the object to receive the notification from the JVM.
   * @param position  the location of a breakpoint.
   * @return the prepare request, or null if the code is managed by this {@code PositionManager} but no class prepare notification is needed
   * @throws NoDataException if the position is not in the code managed by this {@code PositionManager}
   */
  @Nullable
  ClassPrepareRequest createPrepareRequest(@NotNull ClassPrepareRequestor requestor, @NotNull SourcePosition position)
    throws NoDataException;

  /**
   * Return file types this position manager accepts
   *
   * @return set of accepted file types, or null if it accepts all
   */
  default @Nullable Set<? extends FileType> getAcceptedFileTypes() {
    return null;
  }
}
