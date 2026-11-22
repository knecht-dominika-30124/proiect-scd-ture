<template>
  <h1 class="text-h4 mb-4">Bună, {{ currentUser.name }}! 👋</h1>

  <PreferencesCalendar />
</template>

<script setup>
import { currentUser } from '@/session'
import PreferencesCalendar from '@/components/PreferencesCalendar.vue'
</script>