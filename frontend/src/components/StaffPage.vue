<template>
  <h1 class="text-h4 mb-4">Bună, {{ currentUser.name }}! 👋</h1>

  <PreferencesForm />
</template>

<script setup>
import { currentUser } from '@/session'
import PreferencesForm from '@/components/PreferencesForm.vue'
</script>