<script setup>
import { RouterLink } from "vue-router";
</script>

<template>
  <div class="admin-app-shell">
    <header class="admin-header">
      <div class="admin-container">
        <RouterLink to="/" class="admin-brand-link">
          <p class="admin-eyebrow">CHEMILOG</p>
          <h1>Admin CMS</h1>
        </RouterLink>
      </div>
    </header>
    <section class="admin-container">
      <router-view />
    </section>
  </div>
</template>
