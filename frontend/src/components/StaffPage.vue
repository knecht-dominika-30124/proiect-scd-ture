<template>
  <h1 class="text-h4 mb-4">Bună, {{ currentUser.name }}! 👋</h1>

  <v-tabs v-model="tab" color="primary" class="mb-4">
    <v-tab value="shifts" prepend-icon="mdi-calendar-clock">Turele mele</v-tab>
    <v-tab value="preferences" prepend-icon="mdi-calendar-edit">Preferințe</v-tab>
  </v-tabs>

  <v-window v-model="tab">
    <v-window-item value="shifts">
      <MyShiftsCalendar />
    </v-window-item>
    <v-window-item value="preferences">
      <PreferencesCalendar />
    </v-window-item>
  </v-window>
</template>

<script setup>
import { ref } from 'vue'
import { currentUser } from '@/session'
import MyShiftsCalendar from '@/components/MyShiftsCalendar.vue'
import PreferencesCalendar from '@/components/PreferencesCalendar.vue'

const tab = ref('shifts')
</script>