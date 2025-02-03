// Copyright © 2025 Huly Labs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.completion.providers.copilot.lsp

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import java.util.concurrent.CompletableFuture

interface CopilotLSPServer {
  @JsonRequest("initialize")
  fun initialize(params: InitializeParams): CompletableFuture<InitializeResult>

  @JsonRequest("checkStatus")
  fun checkStatus(params: CheckStatusParams = CheckStatusParams()): CompletableFuture<AuthStatusResult>

  @JsonRequest("signInInitiate")
  fun signInInitiate(params: SignInInitiateParams): CompletableFuture<SignInInitiateResult>

  @JsonRequest("signOut")
  fun signOut(params: SignOutParams): CompletableFuture<SignOutResult>

  @JsonRequest("getCompletions")
  fun getCompletions(params: GetCompletionsParams): CompletableFuture<GetCompletionsResult>

  @JsonNotification("initialized")
  fun initialized()

  @JsonNotification("textDocument/didOpen")
  fun didOpenTextDocument(params: DidOpenTextDocumentParams)

  @JsonNotification("textDocument/didClose")
  fun didCloseTextDocument(params: DidCloseTextDocumentParams)

  @JsonNotification("textDocument/didChange")
  fun didChangeTextDocument(params: DidChangeTextDocumentParams)

  @JsonNotification("notifyAccepted")
  fun notifyAccepted(params: NotifyAcceptedParams)

  @JsonNotification("notifyRejected")
  fun notifyRejected(params: NotifyRejectedParams)
}