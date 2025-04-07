<script lang="ts">
  import { on } from "svelte/events";
  import type { Message } from "./types";

  //let role: string = $bindable("user");
  let { role = $bindable("user"), changed = null }: { role: string; changed: any } = $props();

  const roles: string[] = ["user", "assistant", "system"];

  function onclick() {
    const currentIndex = roles.indexOf(role);
    const nextIndex = (currentIndex + 1) % roles.length;
    const newRole = roles[nextIndex];
    role = newRole;
    changed;
  }
</script>

<div class="role-container">
  <button class="role-toggle {role}" {onclick} title="Click to change role: {role}">
    <img src="/assets/icon-{role}.svg" alt={role} width="20" height="20" />
    <span class="role-text">{role}</span>
  </button>
</div>

<style>
  .role-container {
    position: relative;
    padding: 0rem 0.5rem;
  }

  .role-toggle {
    position: relative;
    display: flex;
    align-items: center;
    gap: 4px;
    border: none;
    background-color: transparent;
    color: var(--text-color);
    cursor: pointer;
    transition: all 0.2s;
  }

  .role-toggle.user {
    color: var(--role-user-color);
  }

  .role-toggle.assistant {
    color: var(--role-assistant-color);
  }

  .role-toggle.system {
    color: var(--role-system-color);
  }

  .role-text {
    text-transform: capitalize;
  }

  /* Optional: Add animation for role change */
  .role-toggle {
    transform-origin: bottom center;
  }

  .role-toggle:active {
    transform: scale(0.98);
  }
</style>
