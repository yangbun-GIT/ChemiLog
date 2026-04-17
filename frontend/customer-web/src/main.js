import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import { setupApiClient } from "./api/client";
import "./style.css";

const pinia = createPinia();
setupApiClient(pinia);

const app = createApp(App);
app.use(pinia);
app.use(router);
app.mount("#app");
