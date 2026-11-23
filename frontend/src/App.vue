<template>
  <v-app>
    <v-app-bar color="primary">
      <v-app-bar-title>Gestionarea turelor</v-app-bar-title>

      <template v-if="currentUser">
        <span class="mr-4">
          {{ currentUser.name }} ({{ currentUser.role === 'MANAGER' ? 'manager' : 'angajat' }})
        </span>
        <v-btn variant="outlined" prepend-icon="mdi-logout" @click="logout">Ieșire</v-btn>
      </template>
    </v-app-bar>

    <v-main>
      <v-container>
        <LoginPage v-if="!currentUser" />
        <ManagerPage v-else-if="currentUser.role === 'MANAGER'" />
        <StaffPage v-else />
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup>
import NotificationBell from '@/components/NotificationBell.vue'
import { currentUser, logout } from '@/session'
import LoginPage from '@/components/LoginPage.vue'
import StaffPage from '@/components/StaffPage.vue'
import ManagerPage from '@/components/ManagerPage.vue'
</script>

<template v-if="currentUser">
  <NotificationBell :key="currentUser.id" />
  <span class="mr-4">
          {{ currentUser.name }} ({{ currentUser.role === 'MANAGER' ? 'manager' : 'angajat' }})
        </span>
  <v-btn variant="outlined" prepend-icon="mdi-logout" @click="logout">Ieșire</v-btn>
</template>