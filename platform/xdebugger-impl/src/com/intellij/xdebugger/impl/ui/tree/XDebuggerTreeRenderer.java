// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.xdebugger.impl.ui.tree;

import com.intellij.ide.HelpTooltipManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.frame.ImmediateFullValueEvaluator;
import com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import com.intellij.xdebugger.impl.ui.tree.nodes.XDebuggerTreeNode;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import icons.PlatformDebuggerImplIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

import static com.intellij.util.ui.tree.TreeUtil.getNodeRowX;

public class XDebuggerTreeRenderer extends ColoredTreeCellRenderer {
  private final MyColoredTreeCellRenderer myLink = new MyColoredTreeCellRenderer();
  private final Project myProject;
  private boolean myHaveLink;
  private int myLinkOffset;
  private int myLinkWidth;
  private Object myIconTag;

  private Supplier<String> myLinkShortcutSupplier = null;

  private final MyLongTextHyperlink myLongTextLink = new MyLongTextHyperlink();

  public XDebuggerTreeRenderer(@NotNull Project project) {
    myProject = project;
    getIpad().right = 0;
    myLink.getIpad().left = 0;
    myUsedCustomSpeedSearchHighlighting = true;
  }

  @Override
  public void customizeCellRenderer(final @NotNull JTree tree,
                                    final Object value,
                                    final boolean selected,
                                    final boolean expanded,
                                    final boolean leaf,
                                    final int row,
                                    final boolean hasFocus) {
    myHaveLink = false;
    myLink.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    XDebuggerTreeNode node = (XDebuggerTreeNode)value;
    node.appendToComponent(this);
    updateIcon(node);
    myIconTag = node.getIconTag();

    Rectangle treeVisibleRect = tree.getParent() instanceof JViewport ? ((JViewport)tree.getParent()).getViewRect() : tree.getVisibleRect();
    int rowX = getNodeRowX(tree, row) + tree.getInsets().left;

    // Renderer is not in the hierarchy yet, so we need to set FRC etc. manually
    AppUIUtil.targetToDevice(this, tree);

    if (myHaveLink) {
      setupLinkDimensions(treeVisibleRect, rowX);
    }
    else {
      int visibleRectRightX = treeVisibleRect.x + treeVisibleRect.width;
      int notFittingWidth = rowX + super.getPreferredSize().width - visibleRectRightX;
      if (node instanceof XValueNodeImpl && notFittingWidth > 0) {
        // text does not fit visible area - show link
        String rawValue = DebuggerUIUtil.getNodeRawValue((XValueNodeImpl)node);
        if (!StringUtil.isEmpty(rawValue) && tree.isShowing()) {
          Point treeRightSideOnScreen = new Point(visibleRectRightX, treeVisibleRect.y);
          SwingUtilities.convertPointToScreen(treeRightSideOnScreen, tree);
          Rectangle screen = ScreenUtil.getScreenRectangle(treeRightSideOnScreen);
          // text may fit the screen in ExpandableItemsHandler
          if (screen.x + screen.width < treeRightSideOnScreen.x + notFittingWidth) {
            myLongTextLink.setupComponent(rawValue, myProject);
            append(myLongTextLink.getLinkText(), myLongTextLink.getTextAttributes(), myLongTextLink);
            setupLinkDimensions(treeVisibleRect, rowX);
            myLinkWidth = 0;
          }
        }
      }
    }
    putClientProperty(ExpandableItemsHandler.RENDERER_DISABLED, myHaveLink);
    SpeedSearchUtil.applySpeedSearchHighlightingFiltered(tree, value, this, false, selected);
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    // shortcut should not be shown when there is no link
    if (!myHaveLink && myLinkShortcutSupplier != null) {
      Supplier<String> supplier = ClientProperty.get(myTree, HelpTooltipManager.SHORTCUT_PROPERTY);
      if (supplier == myLinkShortcutSupplier) {
        ClientProperty.remove(myTree, HelpTooltipManager.SHORTCUT_PROPERTY);
        myLinkShortcutSupplier = null;
      }
    }

    String toolTip = myLink.getToolTipText();
    if (isInLinkArea(event.getX()) && toolTip != null) {
      return toolTip;
    }

    return super.getToolTipText(event);
  }

  private boolean isInLinkArea(int x) {
    int linkXCoordinate = x - myLinkOffset;
    if (linkXCoordinate < 0) {
      return false;
    }

    int index = myLink.findFragmentAt(linkXCoordinate);
    if (index == SimpleColoredComponent.FRAGMENT_ICON) {
      return true;
    }

    return index >= 0 && myLink.getFragmentTag(index) != null;
  }

  private void updateIcon(XDebuggerTreeNode node) {
    Icon icon = node instanceof XValueNodeImpl &&
                node.getTree().getPinToTopManager().isEnabled() &&
                node.getTree().getPinToTopManager().isItemPinned((XValueNodeImpl)node) ?
                PlatformDebuggerImplIcons.PinToTop.PinnedItem : node.getIcon();
    setIcon(icon);
  }

  private void setupLinkDimensions(Rectangle treeVisibleRect, int rowX) {
    Dimension linkSize = myLink.getPreferredSize();
    myLinkWidth = linkSize.width;
    myLinkOffset = Math.min(super.getPreferredSize().width, treeVisibleRect.x + treeVisibleRect.width - myLinkWidth - rowX);
    myLink.setSize(myLinkWidth, getHeight()); // actually we only set width here, height is not yet ready
  }

  @Override
  public void append(@NotNull String fragment, @NotNull SimpleTextAttributes attributes, Object tag) {
    if (tag instanceof XDebuggerTreeNodeHyperlink tagValue && tagValue.alwaysOnScreen()) {
      myHaveLink = true;
      myLink.append(fragment, attributes, tag);

      Icon icon = tagValue.getLinkIcon();
      if (icon != null) {
        myLink.setIcon(icon);
      }

      String tooltipText = tagValue.getLinkTooltip();
      if (tooltipText != null) {
        myLink.setToolTipText(tooltipText);

        Supplier<String> shortcutSupplier = tagValue.getShortcutSupplier();
        if (shortcutSupplier != null) {
          myLinkShortcutSupplier = shortcutSupplier;
          ClientProperty.put(myTree, HelpTooltipManager.SHORTCUT_PROPERTY, myLinkShortcutSupplier);
        }
      }
    }
    else {
      super.append(fragment, attributes, tag);
    }
  }

  @Override
  protected void doPaint(Graphics2D g) {
    if (myHaveLink) {
      Graphics2D textGraphics = (Graphics2D)g.create(0, 0, myLinkOffset, getHeight());
      try {
        super.doPaint(textGraphics);
      }
      finally {
        textGraphics.dispose();
      }
      g.translate(myLinkOffset, 0);
      myLink.setSize(myLink.getWidth(), getHeight());
      myLink.doPaint(g);
      g.translate(-myLinkOffset, 0);
    }
    else {
      super.doPaint(g);
    }
  }

  @Override
  public @NotNull Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    if (myHaveLink) {
      size.width += myLinkWidth;
    }
    return size;
  }

  @Override
  public @Nullable Object getFragmentTagAt(int x) {
    if (myHaveLink) {
      Object linkTag = myLink.getFragmentTagAt(x - myLinkOffset);
      if (linkTag != null) {
        return linkTag;
      }
    }
    Object baseFragment = super.getFragmentTagAt(x);
    if (baseFragment != null) {
      return baseFragment;
    }
    if (myIconTag != null && findFragmentAt(x) == FRAGMENT_ICON) {
      return myIconTag;
    }
    return null;
  }

  private static class MyColoredTreeCellRenderer extends ColoredTreeCellRenderer {

    MyColoredTreeCellRenderer() {
      myUsedCustomSpeedSearchHighlighting = true;
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) { }

    @Override
    protected void doPaint(Graphics2D g) {
      super.doPaint(g);
    }
  }

  private static class MyLongTextHyperlink extends XDebuggerTreeNodeHyperlink {
    private String myText;
    private Project myProject;

    MyLongTextHyperlink() {
      super(XDebuggerBundle.message("node.test.show.full.value"));
    }

    public void setupComponent(String text, Project project) {
      myText = text;
      myProject = project;
    }

    @Override
    public boolean alwaysOnScreen() {
      return true;
    }

    @Override
    public void onClick(MouseEvent event) {
      DebuggerUIUtil.showValuePopup(new ImmediateFullValueEvaluator(myText), event, myProject, null);
      event.consume();
    }
  }
}
