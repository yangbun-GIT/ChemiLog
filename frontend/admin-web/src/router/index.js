import { createRouter, createWebHistory } from "vue-router";
import AdminDashboardPage from "../pages/AdminDashboardPage.vue";
import AdminLoginPage from "../pages/AdminLoginPage.vue";
import { useAdminAuthStore } from "../stores/adminAuthStore";

const routes = [
  {
    path: "/",
    name: "admin-dashboard",
    component: AdminDashboardPage,
    meta: { requiresAuth: true },
  },
  {
    path: "/login",
    name: "admin-login",
    component: AdminLoginPage,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const authStore = useAdminAuthStore();

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: "admin-login" };
  }

  if (to.name === "admin-login" && authStore.isAuthenticated) {
    return { name: "admin-dashboard" };
  }

  return true;
});

export default router;
