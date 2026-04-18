import { createRouter, createWebHistory } from "vue-router";
import AdminDashboardPage from "../pages/AdminDashboardPage.vue";
import AdminUsersPage from "../pages/AdminUsersPage.vue";
import AdminFoodsPage from "../pages/AdminFoodsPage.vue";
import AdminAdditivesPage from "../pages/AdminAdditivesPage.vue";
import AdminLogsPage from "../pages/AdminLogsPage.vue";
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
    path: "/users",
    name: "admin-users",
    component: AdminUsersPage,
    meta: { requiresAuth: true },
  },
  {
    path: "/foods",
    name: "admin-foods",
    component: AdminFoodsPage,
    meta: { requiresAuth: true },
  },
  {
    path: "/additives",
    name: "admin-additives",
    component: AdminAdditivesPage,
    meta: { requiresAuth: true },
  },
  {
    path: "/logs",
    name: "admin-logs",
    component: AdminLogsPage,
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
