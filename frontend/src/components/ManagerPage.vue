<template>
  <h1 class="text-h4 mb-4">Panoul managerului</h1>

  <v-tabs v-model="tab" color="primary" class="mb-4">
    <v-tab value="schedule" prepend-icon="mdi-calendar-week">Program</v-tab>
    <v-tab value="employees" prepend-icon="mdi-account-group">Angajați</v-tab>
  </v-tabs>

  <v-window v-model="tab">
    <v-window-item value="schedule">
      <ManagerSchedule />
    </v-window-item>
    <v-window-item value="employees">
      <EmployeeList />
    </v-window-item>
  </v-window>
</template>

<script setup>
import { ref } from 'vue'
import ManagerSchedule from '@/components/ManagerSchedule.vue'
import EmployeeList from '@/components/EmployeeList.vue'

const tab = ref('schedule')
</script>

<template>
  <h1 class="text-h4 mb-4">Panoul managerului</h1>

  <v-tabs v-model="tab" color="primary" class="mb-4">
    <v-tab value="schedule" prepend-icon="mdi-calendar-week">Program</v-tab>
    <v-tab value="timeoff" prepend-icon="mdi-beach">Concedii</v-tab>
    <v-tab value="ai" prepend-icon="mdi-robot-happy-outline">Asistent AI</v-tab>
    <v-tab value="employees" prepend-icon="mdi-account-group">Angajați</v-tab>
  </v-tabs>

  <v-window v-model="tab">
    <v-window-item value="schedule">
      <ManagerSchedule />
    </v-window-item>
    <v-window-item value="timeoff">
      <ManagerTimeOff />
    </v-window-item>
    <v-window-item value="ai">
      <ManagerChat />
    </v-window-item>
    <v-window-item value="employees">
      <EmployeeList />
    </v-window-item>
  </v-window>
</template>

<script setup>
import { ref } from 'vue'
import ManagerSchedule from '@/components/ManagerSchedule.vue'
import ManagerTimeOff from '@/components/ManagerTimeOff.vue'
import ManagerChat from '@/components/ManagerChat.vue'
import EmployeeList from '@/components/EmployeeList.vue'

const tab = ref('schedule')
</script>