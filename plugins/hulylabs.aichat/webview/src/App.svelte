<script lang="ts">
  import ChatWindow from "./lib/ChatWindow.svelte";
  import initScrollHandler from "./lib/scrollHandler";
  import { onMount } from "svelte";

  let chatWindow: ChatWindow;

  onMount(() => {
    initScrollHandler();
  });

  window.onmessage = (event) => {
    let msg = event.data;
    if (msg.type === undefined) {
      return;
    }
    switch (msg.type) {
      case "new-user-message":
        chatWindow.addNewUserMessage(msg.message, msg.systemMessage);
        break;
      case "model-message":
        chatWindow.updateMessage(msg.content, msg.role, msg.isError);
        break;
      case "new-chat":
        chatWindow.newChat();
        break;
      case "add-loaded-message":
        chatWindow.addLoadedMessage(msg.message);
        break;
      case "process-completed":
        chatWindow.processCompleted();
        break;
      case "delete-message":
        chatWindow.deleteChatMessage(msg.id);
        break;
      default:
        break;
    }
  };
</script>

<main>
  <ChatWindow bind:this={chatWindow} />
</main>
