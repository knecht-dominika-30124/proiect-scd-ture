<template>
  <h1 class="text-h4 mb-4">Bună, {{ currentUser.name }}! 👋</h1>
  <v-alert type="info">Aici vor apărea preferințele tale, turele și cererile de concediu.</v-alert>
</template>

<script setup>
import { currentUser } from '@/session'
</script>