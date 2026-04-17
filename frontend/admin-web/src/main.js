import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import "./style.css";
import { setupAdminApi } from "./api/client";
import { useAdminAuthStore } from "./stores/adminAuthStore";

const pinia = createPinia();
const app = createApp(App);

app.use(pinia);

const authStore = useAdminAuthStore(pinia);
authStore.hydrate();
setupAdminApi(() => authStore.accessToken);

app.use(router);
app.mount("#app");
