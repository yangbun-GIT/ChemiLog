import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import { setupApiClient } from "./api/client";
import "./style.css";

document.documentElement.style.backgroundColor = "#f7fcfa";
document.documentElement.style.colorScheme = "light";
document.body.style.backgroundColor = "#f7fcfa";
document.body.style.color = "#0f172a";

const pinia = createPinia();
setupApiClient(pinia);

const app = createApp(App);
app.use(pinia);
app.use(router);
app.mount("#app");
