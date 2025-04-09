// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator

import com.hulylabs.intellij.plugins.langconfigurator.LanguageServerTemplateInstaller.update
import com.hulylabs.intellij.plugins.langconfigurator.messages.HulyLangConfiguratorBundle
import com.hulylabs.intellij.plugins.langconfigurator.templates.HulyLanguageServerTemplateManager
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.redhat.devtools.lsp4ij.LanguageServersRegistry
import com.redhat.devtools.lsp4ij.server.definition.launching.UserDefinedLanguageServerDefinition
import java.util.function.Function
import javax.swing.JComponent

class LanguageServerUpdateNotificationProvider : EditorNotificationProvider {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    val manager = ApplicationManager.getApplication().getService(HulyLanguageServerTemplateManager::class.java)
    val updateRegistry = project.getService(LanguageServerUpdateRegistry::class.java)

    val template = manager.getTemplate(file) ?: return null

    val (serverId, isUpdateSilent) = updateRegistry.getUpdateServerId(template.id)?: return null
    if (isUpdateSilent) {
      return null
    }
    val serverDefinition = LanguageServersRegistry.getInstance().serverDefinitions.find { it.id == serverId } ?: return null
    val userDefinedServerDefinition = serverDefinition as? UserDefinedLanguageServerDefinition ?: return null

    return Function { editor ->
      val notificationPanel = EditorNotificationPanel(editor, EditorNotificationPanel.Status.Info)
      notificationPanel.text = HulyLangConfiguratorBundle.message("message.updatelang.title", template.name)
      val onSuccess = Runnable {
        EditorNotifications.getInstance(project).updateAllNotifications()
        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (psiFile != null) DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
        getNotificationGroup().createNotification(HulyLangConfiguratorBundle.message("message.langconfigurator.title"), HulyLangConfiguratorBundle.message("message.updatelang.configure.success"), NotificationType.INFORMATION).notify(project)
        updateRegistry.resetUpdateNeeded(template.id)
      }
      notificationPanel.createActionLabel(HulyLangConfiguratorBundle.message("message.updatelang.yes"), Runnable {
        update(project, userDefinedServerDefinition, template, onSuccess) { message: String? ->
          getNotificationGroup().createNotification(HulyLangConfiguratorBundle.message("message.langconfigurator.title"), message!!, NotificationType.ERROR).notify(project)
          notificationPanel.isVisible = true
        }
        notificationPanel.isVisible = false
      })
      notificationPanel.createActionLabel(HulyLangConfiguratorBundle.message("message.updatelang.dismiss"), Runnable {
        updateRegistry.resetUpdateNeeded(template.id)
        EditorNotifications.getInstance(project).updateAllNotifications()
      })
      notificationPanel
    }
  }

  private fun getNotificationGroup(): NotificationGroup {
    return NotificationGroupManager.getInstance().getNotificationGroup("Language Server Configuration")
  }
}