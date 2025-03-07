import { mount } from 'svelte'
import './app.css'
import './highlight.css'
import App from './App.svelte'

declare var hulyChat: any;

const app = mount(App, {
  target: document.getElementById('app')!,
})

export default app
