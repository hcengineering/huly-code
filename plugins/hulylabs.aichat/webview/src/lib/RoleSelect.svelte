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

  function getRoleIcon(currentRole: string) {
    switch (currentRole) {
      case "user":
        return `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
          <circle cx="12" cy="7" r="4"></circle>
        </svg>`;
      case "assistant":
        return `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"></path>
        </svg>`;
      case "system":
        return `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="4" y="4" width="16" height="16" rx="2" ry="2"></rect>
          <rect x="9" y="9" width="6" height="6"></rect>
          <line x1="9" y1="1" x2="9" y2="4"></line>
          <line x1="15" y1="1" x2="15" y2="4"></line>
        </svg>`;
      default:
        return "";
    }
  }
</script>

<button class="role-toggle {role}" {onclick} title="Click to change role: {role}">
  {@html getRoleIcon(role)}
  <span class="role-text">{role}</span>
</button>

<style>
  .role-toggle {
    position: relative;
    left: 1em;
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 12px;
    border: 1px solid var(--text-color);
    border-bottom: none;
    background: var(--bg-color);
    color: var(--text-color);
    font-size: 0.75rem;
    cursor: pointer;
    opacity: 1;
    transition: all 0.2s;
    border-radius: 6px 6px 0 0;
    height: 24px;
    z-index: 1;
  }

  .role-toggle::before,
  .role-toggle::after {
    content: "";
    position: absolute;
    bottom: 0;
    width: 0px;
    height: 0px;
    background: var(--bg-color);
  }

  .role-toggle::before {
    left: -6px;
    border-bottom-right-radius: 6px;
    box-shadow: 2px 2px 0 var(--bg-color);
  }

  .role-toggle::after {
    right: -6px;
    border-bottom-left-radius: 6px;
    box-shadow: -2px 2px 0 var(--bg-color);
  }

  .role-toggle:hover {
    opacity: 1;
  }

  .role-toggle.user {
    color: var(--role-user-color);
    border-color: var(--component-border-color);
  }

  .role-toggle.assistant {
    color: var(--role-assistant-color);
    border-color: var(--component-border-color);
  }

  .role-toggle.system {
    color: var(--role-system-color);
    border-color: var(--component-border-color);
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
