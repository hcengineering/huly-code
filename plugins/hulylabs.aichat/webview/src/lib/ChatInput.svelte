<script lang="ts">
  import { onMount } from "svelte";

  let text = $state("");
  let role = $state("user");
  let placeholder = $state("Type a message...");
  let isProcessing = $state(false);
  let { message } = $props();
  let textarea: HTMLTextAreaElement;

  export function setText(s: string) {
    text = s;
  }

  export function requestFocus() {
    textarea.focus();
  }

  export function setProcessing(processing: boolean) {
    isProcessing = processing;
  }

  function handleSubmit() {
    if (text.trim() && !isProcessing) {
      message({ content: text, role });
      text = "";
      textarea.style.height = "auto";
    }
  }

  function handleCancel() {
    message({ content: null, role: null });
  }

  function handleKeydown(event: KeyboardEvent) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      handleSubmit();
    }
  }

  function adjustHeight() {
    textarea.style.height = "auto";
    textarea.style.height = `${textarea.scrollHeight - 32}px`;
  }
</script>

<div class="input-wrapper">
  <div class="input-container">
    <textarea
      bind:value={text}
      bind:this={textarea}
      {placeholder}
      onkeydown={handleKeydown}
      oninput={adjustHeight}
      rows="1"
      disabled={isProcessing}
    ></textarea>
    {#if isProcessing}
      <button onclick={handleCancel} class="cancel-button" aria-label="Cancel processing">
        <img src="/assets/icon-cancel.svg" alt="Cancel" width="32" height="32" />
      </button>
    {:else}
      <button onclick={handleSubmit} class:active={text.trim().length > 0} aria-label="Send message">
        <img src="/assets/icon-send.svg" alt="Send" width="32" height="32" />
      </button>
    {/if}
  </div>
  <div class="input-hint">Shift + Enter a for new line</div>
</div>

<style>
  .input-wrapper {
    position: absolute;
    bottom: 0px;
    left: 0px;
    right: 0px;
    padding: 0rem 1.35rem 0rem 1rem;
    background: linear-gradient(0deg, var(--bg-color) 0% 50%, transparent 70%);
    z-index: 1;
  }

  .input-container {
    position: relative;
    display: flex;
  }

  textarea {
    flex: 1;
    border: 1px solid var(--component-border-color);
    border-radius: 8px;
    padding: 1rem;
    padding-right: 3em;
    background: var(--bg-color);
    color: var(--text-color);
    resize: none;
    min-height: 12px;
    max-height: 200px;
    overflow-y: auto;
    line-height: 1;
    font-family: var(--text-font-family);
    font-size: var(--text-font-size);
  }

  textarea:focus-visible {
    border: 1px solid var(--component-focus-color);
    outline: var(--component-focus-color) outset 1px;
  }

  textarea::placeholder {
    color: var(--text-color);
    opacity: 0.6;
  }

  button {
    position: absolute;
    right: 0rem;
    bottom: 0.5rem;
    background: none;
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-color);
    opacity: 0.5;
    transition: opacity 0.2s;
    z-index: 1;
  }

  button.active {
    opacity: 1;
    color: var(--component-focus-color);
  }

  textarea:disabled {
    cursor: not-allowed;
  }

  .input-hint {
    color: #8f9196;
    padding: 0.5rem 0;
    font-size: 0.75rem;
  }
</style>
