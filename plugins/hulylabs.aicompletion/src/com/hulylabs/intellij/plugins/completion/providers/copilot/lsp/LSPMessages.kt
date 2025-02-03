// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.copilot.lsp

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.net.URI
import java.net.URISyntaxException


typealias VirtualFileUri = String

fun fromFile(file: VirtualFile): VirtualFileUri {
  var uri: String = file.url
  try {
    val prefix = if (SystemInfo.isWindows && file.fileSystem is LocalFileSystem) "/" else ""
    val path = file.path.replace("\\", "/")
    uri = URI(file.fileSystem.protocol, "", "$prefix$path", null).toString()
  }
  catch (_: URISyntaxException) {
    if (file.url.startsWith("file://") && !file.url.startsWith("file:///")) {
      uri = "file:///" + file.url.substring("file://".length)
    }
  }
  return uri
}

object LangIdentifier {
  val langExtensionMap: Map<String, String> = mapOf(
    "bat" to "bat",
    "sh" to "bash",
    "c" to "c",
    "cpp" to "cpp",
    "cs" to "csharp",
    "css" to "css",
    "diff" to "diff",
    "dart" to "dart",
    "dockerfile" to "dockerfile",
    "elixir" to "elixir",
    "erlang" to "erlang",
    "fsharp" to "fsharp",
    "go" to "go",
    "groovy" to "groovy",
    "html" to "html",
    "java" to "java",
    "js" to "javascript",
    "jsx" to "javascriptreact",
    "json" to "json",
    "md" to "markdown",
    "mdx" to "markdown",
    "php" to "php",
    "py" to "python",
    "rs" to "rust",
    "sql" to "sql",
    "ts" to "typescript",
    "tsx" to "typescriptreact",
    "vb" to "vb",
    "xml" to "xml",
    "yaml" to "yaml",
    "yml" to "yaml",
  )
}

data class TextDocumentSyncClientCapabilities(
  val dynamicRegistration: Boolean? = true,
  val willSave: Boolean? = null,
  val willSaveWaitUntil: Boolean? = null,
  val didSave: Boolean? = null,
)

data class TextDocumentClientCapabilities(
  val synchronization: TextDocumentSyncClientCapabilities? = TextDocumentSyncClientCapabilities(),
)

data class WorkspaceCapabilities(
  val workspaceFolders: Boolean = true,
)

data class ClientCapabilities(
  val textDocument: TextDocumentClientCapabilities = TextDocumentClientCapabilities(),
  val workspace: WorkspaceCapabilities = WorkspaceCapabilities(),
  val copilot: CopilotCapabilities = CopilotCapabilities(),
)

data class CopilotCapabilities(
  val fetch: Boolean = true,
  val watchedFiles: Boolean = true,
)

data class NameVersionInfo(
  val name: String,
  val version: String? = null,
)

data class WorkspaceFolder(
  val uri: VirtualFileUri,
)

data class InitializationOptions(
  val editorInfo: NameVersionInfo?,
  val editorPluginInfo: NameVersionInfo?,
  val copilotCapabilities: CopilotCapabilities = CopilotCapabilities(),
  val githubAppId: String? = null,
)

data class InitializeParams(
  val processId: Long,
  val clientInfo: NameVersionInfo?,
  val capabilities: ClientCapabilities = ClientCapabilities(),
  val initializationOptions: InitializationOptions?,
  val workspaceFolders: List<WorkspaceFolder>?,
) {
  constructor(processId: Long, clientInfo: NameVersionInfo?, pluginInfo: NameVersionInfo?, workspaceFolders: List<WorkspaceFolder>?) : this(
    processId,
    clientInfo,
    initializationOptions = InitializationOptions(
      editorInfo = clientInfo,
      editorPluginInfo = pluginInfo,
    ),
    workspaceFolders = workspaceFolders,
  )
}

data class TextDocumentSyncOptions(
  val openClose: Boolean? = null,
  val change: TextDocumentSyncKind? = null,
)

enum class TextDocumentSyncKind {
  NONE,
  FULL,
  INCREMENTAL
}

data class CompletionOptions(
  val resolveProvider: Boolean? = null,
  val allCommitCharacters: List<String>? = null,
  val triggerCharacters: List<String>? = null,
)

data class ServerCapabilities(
  val textDocumentSync: Either<TextDocumentSyncOptions, TextDocumentSyncKind>? = null,
  val completionProvider: CompletionOptions? = null,
)

data class InitializeResult(
  val capabilities: ServerCapabilities,
  val serverInfo: NameVersionInfo?,
)

data class CheckStatusParams(
  val localChecksOnly: Boolean = false,
)

enum class AuthStatusKind {
  OK,
  MaybeOk,
  NotSignedIn,
  NotAuthorized,
  FailedToGetToken,
  TokenInvalid,
}

data class AuthStatusResult(
  val status: AuthStatusKind,
  val user: String,
  val errorMessage: String?,
)

data object SignOutParams
data object SignOutResult

data object SignInInitiateParams

enum class SignInInitiateResultStatus {
  AlreadySignedIn,
  PromptUserDeviceFlow
}

data class SignInInitiateResult(
  val status: SignInInitiateResultStatus,
  val user: String?,
  val userCode: String?,
  val verificationUri: String?,
)

data class LogMessageParams(
  val type: Int,
  val message: String,
)

data class FeatureFlagsNotificationParams(
  // restrictedTelemetryEnabled
  val rt: Boolean?,
  // snippyEnabled
  val sn: Boolean?,
  // chatEnabled
  val chat: Boolean?,
  // inlineChatEnabled
  val ic: Boolean?,
  // unknown feature
  val ep: Boolean?,
  // projectContextEnabled
  val pc: Boolean?,
  // unknown feature
  val x: Boolean?,
)

data class LSPPosition(
  val line: Int,
  val character: Int,
) {
  fun shift(text: String): LSPPosition {
    if (text.isEmpty()) return this
    val offset = StringUtil.offsetToLineColumn(text, text.length)
    return LSPPosition(line + offset.line, if (offset.line == 0) character + offset.column else offset.column)
  }
}

data class LSPRange(
  val start: LSPPosition,
  val end: LSPPosition,
)

data class GetCompletionsDocument(
  val tabSize: Int,
  val indentSize: Int,
  val insertSpaces: Boolean,
  val uri: String,
  val relativePath: String,
  val position: LSPPosition,
  val version: Long,
)

data class GetCompletionsParams(
  val doc: GetCompletionsDocument,
)

data class GetCompletionsResult(
  val completions: List<Completion>,
)

data class Completion(
  val text: String,
  val position: LSPPosition,
  val uuid: String,
  val range: LSPRange,
  val displayText: String,
)

data class TextDocumentItem(
  val uri: VirtualFileUri,
  val languageId: String,
  val version: Int,
  val text: String,
)

data class DidOpenTextDocumentParams(
  val textDocument: TextDocumentItem,
)

data class TextDocumentIdentifier(
  val uri: VirtualFileUri,
)

data class DidCloseTextDocumentParams(
  val textDocument: TextDocumentIdentifier,
) {
  constructor(file: VirtualFile) : this(TextDocumentIdentifier(fromFile(file)))
}

data class VersionedTextDocumentIdentifier(
  val uri: VirtualFileUri,
  val version: Long,
)

data class TextDocumentContentChangeEvent(
  val text: String,
  val range: LSPRange? = null,
)

data class DidChangeTextDocumentParams(
  val textDocument: VersionedTextDocumentIdentifier,
  val contentChanges: List<TextDocumentContentChangeEvent>,
)

data class StatusNotificationParams(
  val status: Either<AuthStatusKind, String>?,
  val message: String,
)

data class NotifyAcceptedParams(
  val uuid: String,
)

data class NotifyRejectedParams(
  val uuids: List<String>,
)

data class MessageActionItem(
  val title: String,
)

enum class MessageType {
  Default,
  Error,
  Warning,
  Info,
  Log,
  Debug,
}

data class ShowMessageRequestParams(
  val type: MessageType,
  val message: String,
  val actions: List<MessageActionItem>?,
)