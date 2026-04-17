import { createRouter, createWebHistory } from "vue-router";
import FoodSearchPage from "../pages/FoodSearchPage.vue";
import CartPage from "../pages/CartPage.vue";
import LoginPage from "../pages/LoginPage.vue";
import MyPage from "../pages/MyPage.vue";

const routes = [
  {
    path: "/",
    name: "food-search",
    component: FoodSearchPage,
  },
  {
    path: "/cart",
    name: "cart",
    component: CartPage,
  },
  {
    path: "/login",
    name: "login",
    component: LoginPage,
  },
  {
    path: "/mypage",
    name: "mypage",
    component: MyPage,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
