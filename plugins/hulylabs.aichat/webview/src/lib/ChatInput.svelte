<script lang="ts">
  import RoleSelect from "./RoleSelect.svelte";
  /*
  const dispatch = createEventDispatcher<{
    send: { text: string; role: string };
  }>();
*/
  let text = $state("");
  let role = $state("user");
  let { message } = $props();
  let textarea: HTMLTextAreaElement;

  export function setText(s: string) {
    text = s;
  }

  export function requestFocus() {
    textarea.focus();
  }

  function handleSubmit() {
    if (text.trim()) {
      message({ content: text, role });
      //$host().dispatchEvent(new CustomEvent({ text, role }));
      text = "";
      textarea.style.height = "auto";
    }
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
    <RoleSelect bind:role changed />
    <textarea
      bind:value={text}
      bind:this={textarea}
      placeholder="Type a message... (Shift + Enter for new line)"
      onkeydown={handleKeydown}
      oninput={adjustHeight}
      rows="1"
    ></textarea>
    <button onclick={handleSubmit} class:active={text.trim().length > 0} aria-label="Send message">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <line x1="22" y1="2" x2="11" y2="13"></line>
        <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
      </svg>
    </button>
  </div>
</div>

<style>
  .input-wrapper {
    position: relative;
  }

  .input-container {
    position: relative;
    display: flex;
    padding: 0 0.3em 0.3em 0.3em;
  }

  .input-container :global(.role-toggle) {
    top: -24px;
    position: absolute;
  }

  textarea {
    flex: 1;
    border: 1px solid var(--component-border-color);
    border-radius: 5px;
    padding: 16px;
    padding-right: 40px;
    background: var(--bg-color);
    color: var(--text-color);
    resize: none;
    min-height: 12px;
    max-height: 200px;
    overflow-y: auto;
    line-height: 1;
    font-family: inherit;
    font-size: 1rem;
    z-index: 1;
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
    right: 1rem;
    bottom: 12px;
    background: none;
    border: none;
    cursor: pointer;
    padding: 0.5rem;
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

  button svg {
    width: 24px;
    height: 24px;
  }
</style>
