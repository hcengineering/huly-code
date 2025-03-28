// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.editor.impl;

import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.util.DocumentUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

import static com.intellij.openapi.editor.impl.InlayKeys.ID_BEFORE_DISPOSAL;
import static com.intellij.openapi.editor.impl.InlayKeys.ORDER_BEFORE_DISPOSAL;

/**
 * @see InlayModel#addInlineElement
 */
final class InlineInlayImpl<R extends EditorCustomElementRenderer> extends InlayImpl<R, InlineInlayImpl<?>> {
  final int myPriority;

  InlineInlayImpl(@NotNull EditorImpl editor,
                  int offset,
                  boolean relatesToPrecedingText,
                  int priority,
                  @NotNull R renderer) {
    super(editor, offset, relatesToPrecedingText, renderer);
    myPriority = priority;
  }

  @Override
  public RangeMarkerTree<InlineInlayImpl<?>> getTree() {
    return myEditor.getInlayModel().myInlineElementsTree;
  }

  @Override
  protected void changedUpdateImpl(@NotNull DocumentEvent e) {
    myEditor.getInlayModel().myPutMergedIntervalsAtBeginning = intervalStart() == e.getOffset();
    super.changedUpdateImpl(e);
    if (isValid() && DocumentUtil.isInsideSurrogatePair(getDocument(), intervalStart())) {
      invalidate();
    }
  }

  @Override
  protected void onReTarget(@NotNull DocumentEvent e) {
    InlayModelImpl inlayModel = myEditor.getInlayModel();
    inlayModel.myPutMergedIntervalsAtBeginning = intervalStart() == e.getMoveOffset() + e.getNewLength();
    if (DocumentUtil.isInsideSurrogatePair(getDocument(), getOffset())) {
      inlayModel.myMoveInProgress = true;
      try {
        invalidate();
      }
      finally {
        inlayModel.myMoveInProgress = false;
      }
    }
  }

  @Override
  public void dispose() {
    if (isValid()) {
      int offset = getOffset();
      List<Inlay<?>> inlays = myEditor.getInlayModel().getInlineElementsInRange(offset, offset);
      putUserData(ORDER_BEFORE_DISPOSAL, inlays.indexOf(this));
      putUserData(ID_BEFORE_DISPOSAL, getId());
    }
    super.dispose();
  }

  @Override
  void doUpdate() {
    myWidthInPixels = myRenderer.calcWidthInPixels(this);
    if (myWidthInPixels <= 0) {
      throw PluginException.createByClass("Positive width should be defined for an inline element by " + myRenderer, null,
                                          myRenderer.getClass());
    }
  }

  @Override
  public @NotNull Placement getPlacement() {
    return Placement.INLINE;
  }

  @Override
  public @NotNull VisualPosition getVisualPosition() {
    int offset = getOffset();
    VisualPosition pos = myEditor.offsetToVisualPosition(offset);
    List<Inlay<?>> inlays = myEditor.getInlayModel().getInlineElementsInRange(offset, offset);
    int order = inlays.indexOf(this);
    return new VisualPosition(pos.line, pos.column + order, true);
  }

  @Override
  Point getPosition() {
    VisualPosition pos = getVisualPosition();
    return myEditor.visualPositionToXY(pos);
  }

  @Override
  public int getHeightInPixels() {
    return myEditor.getLineHeight();
  }

  int getOrder() {
    Integer value = getUserData(ORDER_BEFORE_DISPOSAL);
    return value == null ? -1 : value;
  }

  @Override
  public @NotNull InlayProperties getProperties() {
    return new InlayProperties()
      .relatesToPrecedingText(isRelatedToPrecedingText())
      .disableSoftWrapping(true)
      .priority(myPriority);
  }

  @Override
  public String toString() {
    return "[Inline inlay, offset=" + getOffset() + ", width=" + myWidthInPixels + ", renderer=" + myRenderer + "]" + (isValid() ? "" : "(invalid)");
  }
}
