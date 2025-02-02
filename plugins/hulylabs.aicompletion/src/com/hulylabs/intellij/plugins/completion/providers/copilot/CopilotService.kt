// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.copilot

import com.hulylabs.intellij.plugins.completion.CompletionProviderStateChangedListener
import com.hulylabs.intellij.plugins.completion.providers.copilot.actions.SignInDialog
import com.hulylabs.intellij.plugins.completion.providers.copilot.lsp.*
import com.hulylabs.intellij.plugins.langconfigurator.nodejs.NodeRuntime
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification

private val LOG = Logger.getInstance("#copilot")

enum class AgentStatus {
  Starting,
  Started,
  Stopped,
  SignInProcess,
}

@Service(Service.Level.PROJECT)
class CopilotService(private val project: Project, private val scope: CoroutineScope) {
  private var agent: CopilotAgent? = null
    get() {
      if (field?.isAlive() != true) {
        field = null
        agentStatus = AgentStatus.Stopped
      }
      return field
    }
  private var lastCompletionUUIDs = ArrayList<String>()
  private var agentStatus = AgentStatus.Stopped
  private var signInDialog: SignInDialog? = null

  companion object {
    @JvmStatic
    fun getInstance(project: Project): CopilotService = project.service()
  }

  fun start() {
    scope.launch {
      agentStatus = AgentStatus.Starting
      notifyStateChanged()
      val agentPath =
        try {
          withContext(Dispatchers.IO) {
            CopilotRuntime().getAgentPath(project)
          }
        }
        catch (e: Exception) {
          LOG.error("failed to get agent path", e)
          agentStatus = AgentStatus.Stopped
          return@launch
        }
      try {
        withContext(Dispatchers.IO) {
          val nodeBinaryPath = NodeRuntime.instance().binaryPath()
          val newAgent = CopilotAgent(project, nodeBinaryPath, agentPath)
          newAgent.start()
          agent = newAgent
        }
      }
      catch (e: Exception) {
        LOG.error("failed to start agent", e)
        agentStatus = AgentStatus.Stopped
        return@launch
      }
      agentStatus = AgentStatus.Started
      EditorFactory.getInstance().allEditors.forEach { editor ->
        if (editor.virtualFile != null && editor.project == project) {
          documentOpened(editor.virtualFile, editor.document.text)
        }
      }
      notifyStateChanged()
    }
  }

  fun stop() {
    if (agent?.isAlive() == true) {
      agent?.destroy()
    }
    agent = null
  }

  fun documentOpened(file: VirtualFile, content: String) {
    var langId = ""
    if (file.fileType is LanguageFileType) {
      langId = (file.fileType as LanguageFileType).language.id.lowercase()
    }
    if (langId.isEmpty() || langId == "treesitter" && LangIdentifier.langExtensionMap.contains(file.extension)) {
      langId = LangIdentifier.langExtensionMap[file.extension] ?: ""
    }
    val item = TextDocumentItem(fromFile(file), langId, 0, content)
    agent?.server?.didOpenTextDocument(DidOpenTextDocumentParams(item))
  }

  fun documentClosed(file: VirtualFile) {
    agent?.server?.didCloseTextDocument(DidCloseTextDocumentParams(file))
  }

  fun documentChanged(file: VirtualFile, event: DocumentEvent) {
    val textDocument = VersionedTextDocumentIdentifier(fromFile(file), event.document.modificationStamp)
    if (event.isWholeTextReplaced) {
      agent?.server?.didChangeTextDocument(DidChangeTextDocumentParams(textDocument, listOf(TextDocumentContentChangeEvent(event.document.text))))
    }
    else {
      val fragment = event.newFragment.toString()
      val line = event.document.getLineNumber(event.offset)
      var startPosition = LSPPosition(line, event.offset - event.document.getLineStartOffset(line))
      var endPosition = startPosition.shift(fragment)
      val range = LSPRange(startPosition, endPosition)
      agent?.server?.didChangeTextDocument(DidChangeTextDocumentParams(textDocument, listOf(TextDocumentContentChangeEvent(fragment, range))))
    }
  }

  suspend fun completion(file: VirtualFile, document: Document, cursorOffset: Int): String? {
    val line = document.getLineNumber(cursorOffset)
    val position = LSPPosition(line, cursorOffset - document.getLineStartOffset(line))
    val relativePath = VfsUtil.getRelativePath(file, project.guessProjectDir()!!)!!
    val doc = GetCompletionsDocument(2, 2, false, fromFile(file), relativePath, position, document.modificationStamp)
    val completionResult = agent?.server?.getCompletions(GetCompletionsParams(doc))?.await()
    completionResult?.let {
      if (it.completions.isNotEmpty()) {
        lastCompletionUUIDs.addAll(it.completions.map { it.uuid })
        return it.completions[0].displayText
      }
    }
    return null
  }

  fun completionAccepted() {
    if (lastCompletionUUIDs.isNotEmpty()) {
      agent?.server?.notifyAccepted(NotifyAcceptedParams(lastCompletionUUIDs.first()))
      lastCompletionUUIDs.clear()
    }
  }

  fun completionRejected() {
    if (lastCompletionUUIDs.isNotEmpty()) {
      agent?.server?.notifyRejected(NotifyRejectedParams(lastCompletionUUIDs))
      lastCompletionUUIDs.clear()
    }
  }

  private fun notifyStateChanged() {
    project.messageBus.syncPublisher(CompletionProviderStateChangedListener.TOPIC).stateChanged()
  }

  fun getAgentStatus(): AgentStatus {
    return agentStatus
  }

  fun getAuthStatus(): AuthStatusKind? {
    return agent?.authStatus
  }

  fun signIn() {
    agent?.server?.signInInitiate(SignInInitiateParams)?.thenAccept { result ->
      LOG.info("signIn ${result.status}")
      when (result.status) {
        SignInInitiateResultStatus.AlreadySignedIn -> {
          agent?.authStatus = AuthStatusKind.OK
        }
        SignInInitiateResultStatus.PromptUserDeviceFlow -> {
          agent?.authStatus = AuthStatusKind.NotSignedIn
          agentStatus = AgentStatus.SignInProcess
          ApplicationManager.getApplication().invokeLater {
            signInDialog = SignInDialog(project, result.userCode!!, result.verificationUri!!)
            signInDialog!!.show()
          }
          scope.launch {
            while (agentStatus == AgentStatus.SignInProcess) {
              val status = agent?.server?.checkStatus()?.await()
              LOG.info("checkStatus ${status?.status}")
              if (status?.status != null) {
                if (status.status != AuthStatusKind.NotSignedIn) {
                  agentStatus = AgentStatus.Started
                  withContext(Dispatchers.EDT) {
                    signInDialog?.close(DialogWrapper.OK_EXIT_CODE)
                    signInDialog = null
                  }
                }
                agent?.authStatus = status.status
              }
              else {
                agentStatus = AgentStatus.Started
              }
              notifyStateChanged()
              delay(2000)
            }
          }
        }
      }
    }
  }

  fun logout() {
    agent?.server?.signOut(SignOutParams)?.thenAccept {
      LOG.info("logout success")
      agent?.authStatus = AuthStatusKind.NotSignedIn
      notifyStateChanged()
    }
  }

  // region LSP client methods
  @Suppress("unused")
  @JsonNotification("window/logMessage")
  fun logMessage(params: LogMessageParams) {
    when (params.type) {
      1 -> LOG.error(params.message)
      2 -> LOG.warn(params.message)
      3 -> LOG.info(params.message)
      4 -> LOG.debug(params.message)
      else -> LOG.trace(params.message)
    }
  }

  @Suppress("unused")
  @JsonNotification("window/showMessageRequest")
  fun showMessage(params: ShowMessageRequestParams) {
    val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("AI Inline Completion")
    val notificationType = when (params.type) {
      MessageType.Error -> NotificationType.ERROR
      MessageType.Warning -> NotificationType.WARNING
      else -> NotificationType.INFORMATION
    }
    val notification = notificationGroup.createNotification("GitHub copilot", params.message, notificationType)
    for (action in params.actions ?: emptyList()) {
      if (action.title == "Dismiss") {
        notification.addAction(NotificationAction.createSimpleExpiring("Dismiss") { notification.expire() })
      }
    }
    notification.notify(project)
  }


  @Suppress("unused")
  @JsonNotification("featureFlagsNotification")
  fun featureFlagsNotification(params: FeatureFlagsNotificationParams) {
    LOG.info("feature flags: ${params}")
  }

  @Suppress("unused")
  @JsonNotification("statusNotification")
  fun statusNotification(params: StatusNotificationParams) {
    if (params.status != null && params.status.isLeft) {
      LOG.info("status: ${params.status} message: ${params.message}")
      agent?.authStatus = params.status.left
      notifyStateChanged()
    }
  } // endregion
}