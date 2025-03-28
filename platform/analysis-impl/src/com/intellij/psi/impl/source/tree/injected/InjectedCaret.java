// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.impl.source.tree.injected;

import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InjectedCaret implements Caret {
  private final EditorWindow myEditorWindow;
  private final Caret myDelegate;

  @ApiStatus.Internal
  public InjectedCaret(@NotNull EditorWindow window, @NotNull Caret delegate) {
    myEditorWindow = window;
    myDelegate = delegate;
  }

  @Override
  public @NotNull Editor getEditor() {
    return myEditorWindow;
  }

  @Override
  public @NotNull CaretModel getCaretModel() {
    return myEditorWindow.getCaretModel();
  }

  public @NotNull Caret getDelegate() {
    return myDelegate;
  }

  @Override
  public boolean isValid() {
    return myDelegate.isValid();
  }

  @Override
  public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean scrollToCaret) {
    myDelegate.moveCaretRelatively(columnShift, lineShift, withSelection, scrollToCaret);
  }

  @Override
  public void moveToLogicalPosition(@NotNull LogicalPosition pos) {
    LogicalPosition hostPos = myEditorWindow.injectedToHost(pos);
    myDelegate.moveToLogicalPosition(hostPos);
  }

  @Override
  public void moveToVisualPosition(@NotNull VisualPosition pos) {
    LogicalPosition hostPos = myEditorWindow.injectedToHost(myEditorWindow.visualToLogicalPosition(pos));
    myDelegate.moveToLogicalPosition(hostPos);
  }

  @Override
  public void moveToOffset(int offset) {
    moveToOffset(offset, false);
  }

  @Override
  public void moveToOffset(int offset, boolean locateBeforeSoftWrap) {
    int hostOffset = myEditorWindow.getDocument().injectedToHost(offset);
    myDelegate.moveToOffset(hostOffset, locateBeforeSoftWrap);
  }

  @Override
  public boolean isUpToDate() {
    return myDelegate.isUpToDate();
  }

  @Override
  public @NotNull LogicalPosition getLogicalPosition() {
    LogicalPosition hostPos = myDelegate.getLogicalPosition();
    return myEditorWindow.hostToInjected(hostPos);
  }

  @Override
  public @NotNull VisualPosition getVisualPosition() {
    LogicalPosition logicalPosition = getLogicalPosition();
    return myEditorWindow.logicalToVisualPosition(logicalPosition);
  }

  @Override
  public int getOffset() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getOffset());
  }

  @Override
  public int getVisualLineStart() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getVisualLineStart());
  }

  @Override
  public int getVisualLineEnd() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getVisualLineEnd());
  }

  @Override
  public int getSelectionStart() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getSelectionStart());
  }

  @Override
  public @NotNull VisualPosition getSelectionStartPosition() {
    return myDelegate.getSelectionStartPosition();
  }

  @Override
  public int getSelectionEnd() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getSelectionEnd());
  }

  @Override
  public @NotNull VisualPosition getSelectionEndPosition() {
    return myDelegate.getSelectionEndPosition();
  }

  @Override
  public @Nullable String getSelectedText() {
    return myDelegate.getSelectedText();
  }

  @Override
  public int getLeadSelectionOffset() {
    return myEditorWindow.getDocument().hostToInjected(myDelegate.getLeadSelectionOffset());
  }

  @Override
  public @NotNull VisualPosition getLeadSelectionPosition() {
    return myDelegate.getLeadSelectionPosition();
  }

  @Override
  public boolean hasSelection() {
    return myDelegate.hasSelection();
  }

  @Override
  public @NotNull TextRange getSelectionRange() {
    TextRange range = myDelegate.getSelectionRange();
    return TextRange.create(myEditorWindow.getDocument().hostToInjected(range.getStartOffset()), myEditorWindow.getDocument().hostToInjected(range.getEndOffset()));
  }

  @Override
  public void setSelection(int startOffset, int endOffset) {
    TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
    myDelegate.setSelection(hostRange.getStartOffset(), hostRange.getEndOffset());
  }

  @Override
  public void setSelection(int startOffset, int endOffset, boolean updateSystemSelection) {
    TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
    myDelegate.setSelection(hostRange.getStartOffset(), hostRange.getEndOffset(), updateSystemSelection);
  }

  @Override
  public void setSelection(int startOffset, @Nullable VisualPosition endPosition, int endOffset) {
    TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
    myDelegate.setSelection(hostRange.getStartOffset(), endPosition, hostRange.getEndOffset());
  }

  @Override
  public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset) {
    TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
    myDelegate.setSelection(startPosition, hostRange.getStartOffset(), endPosition, hostRange.getEndOffset());
  }

  @Override
  public void setSelection(@Nullable VisualPosition startPosition, int startOffset, @Nullable VisualPosition endPosition, int endOffset, boolean updateSystemSelection) {
    TextRange hostRange = myEditorWindow.getDocument().injectedToHost(new ProperTextRange(startOffset, endOffset));
    myDelegate.setSelection(startPosition, hostRange.getStartOffset(), endPosition, hostRange.getEndOffset(), updateSystemSelection);
  }

  @Override
  public void removeSelection() {
    myDelegate.removeSelection();
  }

  @Override
  public void selectLineAtCaret() {
    myDelegate.selectLineAtCaret();
  }

  @Override
  public void selectWordAtCaret(boolean honorCamelWordsSettings) {
    myDelegate.selectWordAtCaret(honorCamelWordsSettings);
  }

  @Override
  public @Nullable Caret clone(boolean above) {
    Caret clone = myDelegate.clone(above);
    return clone == null ? null : new InjectedCaret(myEditorWindow, clone);
  }

  @Override
  public void dispose() {
    //noinspection SSBasedInspection
    myDelegate.dispose();
  }

  @Override
  public @NotNull <T> T putUserDataIfAbsent(@NotNull Key<T> key, @NotNull T value) {
    return myDelegate.putUserDataIfAbsent(key, value);
  }

  @Override
  public <T> boolean replace(@NotNull Key<T> key, @Nullable T oldValue, @Nullable T newValue) {
    return myDelegate.replace(key, oldValue, newValue);
  }

  @Override
  public @Nullable <T> T getUserData(@NotNull Key<T> key) {
    return myDelegate.getUserData(key);
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    myDelegate.putUserData(key, value);
  }

  @Override
  public boolean isAtRtlLocation() {
    return myDelegate.isAtRtlLocation();
  }

  @Override
  public boolean isAtBidiRunBoundary() {
    return myDelegate.isAtBidiRunBoundary();
  }

  @Override
  public @NotNull CaretVisualAttributes getVisualAttributes() {
    return myDelegate.getVisualAttributes();
  }

  @Override
  public void setVisualAttributes(@NotNull CaretVisualAttributes attributes) {
    myDelegate.setVisualAttributes(attributes);
  }
}
