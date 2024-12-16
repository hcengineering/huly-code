// Copyright © 2024 HulyLabs. Use of this source code is governed by the Apache 2.0 license.
package hulylabs.hulycode.plugins.hulylangconfigurator;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.EditorNotifications;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import hulylabs.hulycode.plugins.hulylangconfigurator.templates.HulyLanguageServerTemplate;
import hulylabs.hulycode.plugins.hulylangconfigurator.templates.HulyLanguageServerTemplateManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Locale;
import java.util.function.Function;

import static hulylabs.hulycode.plugins.hulylangconfigurator.messages.HulyLangConfiguratorBundle.message;


public class LanguageServerConfigSetupNotificationProvider implements EditorNotificationProvider {
  private static final String IGNORED_EXTS_KEY_PREFIX = "hulylangconfigurator.ignoreexts.";

  @Override
  public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project,
                                                                                                                 @NotNull VirtualFile file) {
    String ext = file.getExtension() != null ? file.getExtension().toLowerCase(Locale.ROOT) : "";
    if (PropertiesComponent.getInstance(project).getBoolean(IGNORED_EXTS_KEY_PREFIX + ext)) return null;
    HulyLanguageServerTemplate template = HulyLanguageServerTemplateManager.getInstance().getTemplate(project, file);
    if (template == null) return null;

    if (LanguageServersRegistry.getInstance().isFileSupported(file, project)) return null;

    return fileEditor -> {
      EditorNotificationPanel panel = new EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info);
      panel.setText(message("message.addlang.title"));
      Runnable onSuccess = () -> {
        EditorNotifications.getInstance(project).updateAllNotifications();
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null) DaemonCodeAnalyzer.getInstance(project).restart(psiFile);
        getNotificationGroup().createNotification(message("message.langconfigurator.title"), message("message.addlang.configure.success"),
                                                  NotificationType.INFORMATION).notify(project);
      };
      Runnable onFailure = () -> {
        getNotificationGroup().createNotification(message("message.langconfigurator.title"), message("message.addlang.cannot.configre"),
                                                  NotificationType.ERROR)
          .notify(project);
      };
      panel.createActionLabel(message("message.addlang.yes"), () -> {
        LanguageServerTemplateInstaller.install(project, template, onSuccess, onFailure);
      });
      panel.createActionLabel(message("message.addlang.dismiss"), () -> {
        PropertiesComponent.getInstance(project).setValue(IGNORED_EXTS_KEY_PREFIX + ext, true);
        EditorNotifications.getInstance(project).updateAllNotifications();
      });
      return panel;
    };
  }

  private static NotificationGroup getNotificationGroup() {
    return NotificationGroupManager.getInstance().getNotificationGroup("Language Server Configuration");
  }
}
