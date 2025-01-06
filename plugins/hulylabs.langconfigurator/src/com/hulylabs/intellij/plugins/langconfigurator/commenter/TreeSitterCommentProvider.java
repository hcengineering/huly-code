// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator.commenter;

import com.hulylabs.intellij.plugins.treesitter.language.TreeSitterLanguage;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.templateLanguages.MultipleLangCommentProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.textmate.editor.TextMateCommentProvider;

public class TreeSitterCommentProvider implements MultipleLangCommentProvider, Commenter {
  private static final TextMateCommentProvider textMateCommentProvider = new TextMateCommentProvider();

  @Nullable
  @Override
  public Commenter getLineCommenter(@NotNull PsiFile file,
                                    @NotNull Editor editor,
                                    @NotNull Language lineStartLanguage,
                                    @NotNull Language lineEndLanguage) {
    return textMateCommentProvider.getLineCommenter(file, editor, lineStartLanguage, lineEndLanguage);
  }

  @Override
  public boolean canProcess(@NotNull PsiFile file, @NotNull FileViewProvider viewProvider) {
    return viewProvider.getBaseLanguage() == TreeSitterLanguage.INSTANCE;
  }

  @Nullable
  @Override
  public String getLineCommentPrefix() {
    return null;
  }

  @Nullable
  @Override
  public String getBlockCommentPrefix() {
    return "";
  }

  @Nullable
  @Override
  public String getBlockCommentSuffix() {
    return "";
  }

  @Nullable
  @Override
  public String getCommentedBlockCommentPrefix() {
    return null;
  }

  @Nullable
  @Override
  public String getCommentedBlockCommentSuffix() {
    return null;
  }
}
