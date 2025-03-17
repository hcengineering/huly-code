<script lang="ts">
  import { onMount } from "svelte";
  import type { Message } from "./types";
  import ChatInput from "./ChatInput.svelte";
  import hulyChat from "./hulyChat";
  import { marked } from "marked";
  import Prism from "prismjs";
  import RoleSelect from "./RoleSelect.svelte";

  // Import languages
  import "prismjs/components/prism-typescript";
  import "prismjs/components/prism-javascript";
  import "prismjs/components/prism-jsx";
  import "prismjs/components/prism-tsx";
  import "prismjs/components/prism-css";
  import "prismjs/components/prism-json";
  import "prismjs/components/prism-java";
  import "prismjs/components/prism-kotlin";
  import "prismjs/components/prism-python";
  import "prismjs/components/prism-bash";
  import "prismjs/components/prism-batch";
  import "prismjs/components/prism-cmake";
  import "prismjs/components/prism-docker";
  import "prismjs/components/prism-git";
  import "prismjs/components/prism-markdown";
  import "prismjs/components/prism-yaml";
  import "prismjs/components/prism-sql";
  import "prismjs/components/prism-rust";
  import "prismjs/components/prism-go";
  import "prismjs/components/prism-odin";
  import "prismjs/components/prism-zig";
  import "prismjs/components/prism-toml";

  export let messages: Message[] = [];
  var currentMessage: Message;
  var chatInput: ChatInput;
  var waitingResponse: boolean = false;
  let messagesContainer: HTMLDivElement;

  function scrollToBottom() {
    if (messagesContainer) {
      messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
  }

  export function newChat() {
    messages = [];
    waitingResponse = false;
    chatInput.requestFocus();
  }

  // loaded history messages
  export function addLoadedMessage(message: Message) {
    messages = [...messages, message];
    setTimeout(scrollToBottom, 0);
  }

  // add two messages one from user, the other from system placeholder message
  export function addNewUserMessage(message: Message, systemMessage: Message) {
    currentMessage = systemMessage;
    if (messages.length > 1 && messages[messages.length - 1].isError) {
      messages.pop();
      messages.pop();
    }
    waitingResponse = true;
    messages = [...messages, message, systemMessage];
    setTimeout(scrollToBottom, 0);
  }

  export function updateMessage(content: string, role: string, isError: boolean) {
    currentMessage.content += content;
    currentMessage.role = role;
    currentMessage.isError = isError;
    if (isError && messages.length > 1) {
      chatInput.setText(messages[messages.length - 2].content);
    }
    waitingResponse = false;
    messages = messages;
    setTimeout(scrollToBottom, 0);
  }

  export function processCompleted() {
    chatInput.setProcessing(false);
  }

  function handleMessage(message: { content: string; role: string }) {
    chatInput.setProcessing(true);
    if (message.role == null) {
      window.hulyChat.cancelProcessing();
    } else {
      window.hulyChat.postMessage(message);
    }
  }

  function handleChangeRole(message: Message) {
    hulyChat.setRole(message.id, message.role);
  }

  function highlightCode(code: string, lang: string): string {
    try {
      if (lang && Prism.languages[lang]) {
        return Prism.highlight(code, Prism.languages[lang], lang);
      }
      return code;
    } catch (e) {
      console.error("Highlighting error:", e);
      return code;
    }
  }

  function formatText(text: string) {
    try {
      let thinkIndex = text.indexOf("<think>");
      if (thinkIndex !== -1) {
        let endIndex = text.indexOf("</think>", thinkIndex);
        if (endIndex === -1) endIndex = text.length;

        const beforeThink = text.substring(0, thinkIndex);
        const thinkContent = text.substring(thinkIndex + 7, endIndex);
        const afterThink = endIndex < text.length - 8 ? text.substring(endIndex + 8) : "";

        const id = `think-${Math.random().toString(36).substr(2, 9)}`;

        return (
          marked.parse(beforeThink) +
          `<details class="collapsible-block" id="${id}">
          <summary>Thinking...</summary>
          <div class="collapsible-content">
            ${marked.parse(thinkContent)}
          </div>
        </details>` +
          marked.parse(afterThink)
        );
      }
      return marked.parse(text);
    } catch (e) {
      console.error("Markdown parsing error:", e);
      return text.replace(/\n/g, "<br>");
    }
  }

  onMount(() => {
    hulyChat.setChatWindow({
      newChat,
      addLoadedMessage,
      addNewUserMessage,
      updateMessage,
      processCompleted,
    });

    window.toggleCollapsible = (id: string) => {
      const details = document.getElementById(id) as any;
      if (details) {
        details.open = !details.open;
      }
    };

    window.copyCode = (id: string) => {
      const codeElement = document.getElementById(id);
      if (codeElement) {
        const code = codeElement.innerText;
        hulyChat.copyCode(code).then(
          () => {
            const button = codeElement.parentElement?.querySelector(".copy-button");
            if (button) {
              button.classList.add("copied");
              const tooltip = button.querySelector(".tooltip");
              if (tooltip) {
                tooltip.textContent = "Copied!";
                setTimeout(() => {
                  button.classList.remove("copied");
                  tooltip.textContent = "Copy";
                }, 2000);
              }
            }
          },
          (err) => {
            console.error("Failed to copy:", err);
          },
        );
      }
    };

    marked.use({
      gfm: true,
      breaks: true,
      renderer: {
        code(this: any, arg0: any) {
          const validLanguage = arg0.lang || "text";
          const highlighted = highlightCode(arg0.text, validLanguage);
          const id = `code-${Math.random().toString(36).substr(2, 9)}`;

          return `
        <div class="code-block-wrapper">
          <pre class="language-${validLanguage}"><div class="code-actions">
              <span class="code-lang">${validLanguage}</span>
              <button class="copy-button" onclick="copyCode('${id}')">
                <svg class="copy-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                </svg>
                <span class="tooltip">Copy</span>
              </button>
            </div><code id="${id}" class="language-${validLanguage}">${highlighted}</code></pre>
        </div>`;
        },
      },
    });

    return () => {
      hulyChat.setChatWindow(null);
    };
  });
</script>

<div class="chat-window">
  <div class="messages" bind:this={messagesContainer}>
    {#each messages as message, i (message.id)}
      {@const isLast = i === messages.length - 1}
      <div class="message {message.role}">
        <div class="role-container">
          <RoleSelect bind:role={message.role} changed={handleChangeRole(message)} />
        </div>
        <div class="message-content">
          {#if isLast && waitingResponse}
            <div class="waiting-response">
              <div class="spinner"></div>
              <span>Waiting for response...</span>
            </div>
          {:else if message.isError}
            <div class="error-message">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              <span>{message.content}</span>
            </div>
          {:else}
            {@html formatText(message.content)}
          {/if}
        </div>
      </div>
    {/each}
  </div>
  <ChatInput bind:this={chatInput} message={handleMessage} />
</div>

<style>
  .chat-window {
    height: 100vh;
    margin: 0rem auto;
    border: 0px solid var(--text-color);
    display: flex;
    flex-direction: column;
  }

  .messages {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    position: relative;
    scroll-behavior: smooth;
  }

  .message {
    width: 100%;
    position: relative;
    padding: 24px 0px; /* Space for the role selector */
    margin-top: -24px; /* Overlap with previous message */
    background: var(--chat-bg);
  }

  .message:first-child {
    margin-top: 0; /* Don't overlap for first message */
  }

  .role-container {
    position: absolute;
    top: 1px; /* Pull up to overlap */
    z-index: 2;
  }

  .message-content {
    padding: 0.2em 1rem;
    word-break: break-word;
    position: relative;
    z-index: 1;
    border-top: 1px solid var(--component-border-color);
  }

  .error-message {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 1rem;
    color: #dc3545;
    background-color: rgba(220, 53, 69, 0.1);
    border-radius: 4px;
    margin: 0.5rem 0;
  }

  .error-message svg {
    flex-shrink: 0;
  }

  .error-message span {
    font-size: 0.9em;
    line-height: 1.4;
  }
  /*
  .user .message-content {
    border-top: 1px solid var(--role-user-color);
  }

  .assistant .message-content {
    border-top: 1px solid var(--role-assistant-color);
  }
  .system .message-content {
    border-top: 1px solid var(--role-system-color);
  }

  */
  /* Code block styling */
  .message-content :global(.code-block-wrapper) {
    position: relative;
    margin: 1em 0;
  }

  .message-content :global(.code-actions) {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    display: flex;
    gap: 0.5rem;
    align-items: center;
    opacity: 0;
    transition: opacity 0.2s;
    z-index: 1; /* Add this */
  }

  .message-content :global(.code-actions) {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    display: flex;
    gap: 0.5rem;
    align-items: center;
    opacity: 0;
    transition: opacity 0.2s;
  }

  .message-content :global(.code-block-wrapper:hover .code-actions) {
    opacity: 1;
  }

  .message-content :global(.code-lang) {
    font-size: 0.75em;
    text-transform: uppercase;
    color: var(--text-color);
    opacity: 0.7;
    padding: 0.2em 0.4em;
    background: rgba(var(--text-color-rgb), 0.1);
    border-radius: 3px;
  }

  .message-content :global(.copy-button) {
    background: rgba(var(--text-color-rgb), 0.1);
    border: none;
    color: var(--text-color);
    cursor: pointer;
    padding: 0.25rem;
    border-radius: 3px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
  }

  .message-content :global(.copy-button:hover) {
    background: rgba(var(--text-color-rgb), 0.2);
  }

  .message-content :global(.copy-button.copied) {
    background: rgba(76, 175, 80, 0.2);
    color: #4caf50;
  }

  .message-content :global(.copy-button .tooltip) {
    position: absolute;
    right: 0;
    top: 100%;
    background: var(--text-color);
    color: var(--bg-color);
    padding: 0.25rem 0.5rem;
    border-radius: 3px;
    font-size: 0.75em;
    opacity: 0.6;
    transition: all 0.2s;
    white-space: nowrap;
    pointer-events: none;
    margin-top: 0.25rem;
  }

  .message-content :global(.copy-button:hover .tooltip) {
    opacity: 1;
    visibility: visible;
  }

  .message-content :global(.tooltip) {
    opacity: 0;
    visibility: hidden;
  }

  /* Adjust pre padding to accommodate the button */
  .message-content :global(pre) {
    margin: 0;
    padding: 1em;
    border: 1px solid var(--component-border-color);
    border-radius: 4px;
    overflow-x: auto;
  }

  .message-content :global(pre code) {
    font-family: "JetBrains Mono", "Fira Code", monospace;
    font-size: 0.9em;
    line-height: 1.5;
    display: block;
    padding: 0;
    margin: 0;
  }

  /* Inline code styling */
  .message-content :global(code:not(pre code)) {
    font-family: "JetBrains Mono", "Fira Code", monospace;
    font-size: 0.9em;
    padding: 0.2em 0.4em;
    border-radius: 3px;
    background: var(--inline-code-bg);
  }

  .message-content :global(.collapsible-block) {
    border: 1px solid var(--component-border-color);
    border-radius: 4px;
    margin: 1em 0;
    overflow: hidden;
  }

  .message-content :global(.collapsible-block summary) {
    padding: 0.75em 1em;
    background-color: rgba(var(--text-color-rgb), 0.05);
    cursor: pointer;
    font-weight: 500;
    user-select: none;
  }

  .message-content :global(.collapsible-block summary:hover) {
    background-color: rgba(var(--text-color-rgb), 0.1);
  }

  .message-content :global(.collapsible-content) {
    padding: 0.75em 1em;
    border-top: 1px solid var(--component-border-color);
  }

  /* Basic markdown styles */
  .message-content :global(p) {
    margin: 0.5em 0;
    line-height: 1.5;
  }

  .message-content :global(ul),
  .message-content :global(ol) {
    margin: 0.5em 0;
    padding-left: 1.5em;
  }

  .message-content :global(li) {
    margin: 0.25em 0;
  }

  .message-content :global(h1),
  .message-content :global(h2),
  .message-content :global(h3),
  .message-content :global(h4),
  .message-content :global(h5),
  .message-content :global(h6) {
    margin: 1em 0 0.5em 0;
    line-height: 1.2;
  }

  .message-content :global(a) {
    color: #0366d6;
    text-decoration: none;
  }

  .message-content :global(a:hover) {
    text-decoration: underline;
  }

  .message-content :global(blockquote) {
    margin: 0.5em 0;
    padding-left: 1em;
    border-left: 4px solid var(--text-color);
    opacity: 0.8;
  }

  .message-content :global(table) {
    border-collapse: collapse;
    width: 100%;
    margin: 1em 0;
  }

  .message-content :global(th),
  .message-content :global(td) {
    border: 1px solid var(--text-color);
    padding: 0.5em;
  }

  .message-content :global(img) {
    max-width: 100%;
    height: auto;
  }

  .waiting-response {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 1rem;
    color: var(--text-color);
    opacity: 0.7;
    font-size: 0.9em;
  }

  .spinner {
    width: 16px;
    height: 16px;
    border: 2px solid transparent;
    border-top-color: currentColor;
    border-right-color: currentColor;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }

  /* Optional: Add a pulse animation to the text */
  .waiting-response span {
    animation: pulse 1.5s ease infinite;
  }

  @keyframes pulse {
    0% {
      opacity: 0.5;
    }
    50% {
      opacity: 1;
    }
    100% {
      opacity: 0.5;
    }
  }
</style>
