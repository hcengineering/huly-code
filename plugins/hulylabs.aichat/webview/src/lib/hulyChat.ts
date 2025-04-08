import type { Message } from "./types";

type ChatWindow = {
  newChat: () => void;
  addLoadedMessage: (message: Message) => void;
  addNewUserMessage: (message: Message, systemMessage: Message) => void;
  updateMessage: (content: string, role: string, isError: boolean) => void;
  processCompleted: () => void;
  deleteChatMessage: (id: string) => void;
};

class HulyChat {
  private chatWindow: ChatWindow | null = null;
  private count: number = 0;

  setChatWindow(window: ChatWindow | null) {
    this.chatWindow = window;
  }
  copyCode(code: string): Promise<void> {
    return navigator.clipboard.writeText(code);
  }

  setRole(messageId: string, role: string) {
    //console.log("set role", messageId, role);
  }

  cancelProcessing() {
    // empty block
  }

  deleteChatMessage(id: string) {
    // empty block
  }

  postMessage(message: { content: string; role: string }) {
    //    if (!this.chatWindow) {
    //      console.warn('Chat window not initialized');
    //      return;
    //    }
    //    var newMessage: Message = {
    //      id: (this.count++).toString(),
    //      content: message.content,
    //      role: message.role,
    //      isError: false,
    //    };
    //
    //    var systemMessage: Message = {
    //      id: (this.count++).toString(),
    //      content: "",
    //      role: "system",
    //      isError: false,
    //    };
    //
    //    this.chatWindow.addNewUserMessage(newMessage, systemMessage);
    //    // Simulate system response
    //    let chatWindow = this.chatWindow;
    //    setTimeout(() => {
    //      var msg = "<think>Certainly! Below is a Python program";
    //      chatWindow.updateMessage(msg, 'system', false);
    //    }, 1000);
    //    setTimeout(() => {
    //      var msg = "Certainly! Below is a Python program</think>";
    //      chatWindow.updateMessage(msg, 'system', false);
    //    }, 2000);
    //    setTimeout(() => {
    //      var msg = "Certainly! Below is a Python program that sorts a list of strings alphabetically:\n\n```python\ndef sort_strings(strings):\n    \"\"\"\n    Sorts a list of strings alphabetically.\n\n    Parameters:\n    strings (list): A list of strings to be sorted.\n\n    Returns:\n    list: A new list containing the sorted strings.\n    \"\"\"\n    return sorted(strings)\n\n# Example usage\nif __name__ == \"__main__\":\n    # List of strings to sort\n    unsorted_strings = [\"banana\", \"apple\", \"cherry\", \"date\"]\n\n    # Sort the strings\n    sorted_strings = sort_strings(unsorted_strings)\n\n    # Print the sorted list\n    print(\"Unsorted:\", unsorted_strings)\n    print(\"Sorted:\", sorted_strings)\n```\n\n### Explanation:\n1. **Function Definition**: The `sort_strings` function takes a list of strings as input and returns a new list with the strings sorted alphabetically using Python's built-in `sorted()` function.\n2. **Example Usage**: In the main block, we create a list of unsorted strings, call the `sort_strings` function to sort them, and then print both the original and sorted lists.\n\n### Running the Program:\nTo run this program, simply save it to a file (e.g., `sort_strings.py`) and execute it using Python:\n\n```sh\npython sort_strings.py\n```\n\nThis will output:\n\n```\nUnsorted: ['banana', 'apple', 'cherry', 'date']\nSorted: ['apple', 'banana', 'cherry', 'date']\n```\n\nFeel free to modify the `unsorted_strings` list with your own set of strings!";
    //      chatWindow.updateMessage(msg, 'system', false);
    //      chatWindow.processCompleted();
    //    }, 3000);
  }
}

declare global {
  interface Window {
    hulyChat: HulyChat;
    copyCode: (id: string) => void;
    toggleCollapsible: (id: string) => void;
  }
}

window.hulyChat = window.hulyChat || new HulyChat();

export default window.hulyChat;