// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.

package com.redhat.devtools.lsp4ij.features.documentSymbol;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.features.LSPPsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * File view provider factory for LSP files that yields simple PSI elements for the word under the caret. This helps
 * with a number of plugin SDK EPs that need to find distinct elements.
 */
public class LSPDocumentFileViewProviderFactory implements FileViewProviderFactory {

  @Override
  @NotNull
  public FileViewProvider createFileViewProvider(@NotNull VirtualFile virtualFile,
                                                 Language language,
                                                 @NotNull PsiManager manager,
                                                 boolean eventSystemEnabled) {
    Project project = manager.getProject();
    if (!project.isDisposed() && !virtualFile.isDirectory() && virtualFile.isValid()) {
      // NOTE: We can't filter by LSP here, so we just have to make sure that this view provider behaves as the
      // standard view provider for non-LSP files
      return new LSPDocumentFileViewProvider(manager, virtualFile, eventSystemEnabled);
    }

    return new SingleRootFileViewProvider(manager, virtualFile, eventSystemEnabled);
  }

  private static class LSPDocumentFileViewProvider extends SingleRootFileViewProvider {
    public LSPDocumentFileViewProvider(@NotNull PsiManager manager,
                                       @NotNull VirtualFile virtualFile,
                                       boolean eventSystemEnabled) {
      super(manager, virtualFile, eventSystemEnabled);
    }

    @Override
    public PsiElement findElementAt(int offset) {
      VirtualFile virtualFile = getVirtualFile();
      Project project = getManager().getProject();
      PsiFile file = LSPIJUtils.getPsiFile(virtualFile, project);
      // TODO: How can we quickly and simply check whether this is an LSP4IJ-managed file?
      if (file != null) {
        Document document = LSPIJUtils.getDocument(file);
        if (document != null) {
          TextRange wordTextRange = LSPIJUtils.getWordRangeAt(document, file, offset);
          if (wordTextRange != null) {
            return new LSPPsiElement(file, wordTextRange) {
              @Override
              // NOTE: We have to report this as physical even though it's not
              public boolean isPhysical() {
                return true;
              }
            };
          }
        }
      }

      return super.findElementAt(offset);
    }
  }
}