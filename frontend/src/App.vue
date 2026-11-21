<template>
  <v-app>
    <v-app-bar color="primary" title="Gestionarea turelor" />

    <v-main>
      <v-container>
        <EmployeeList />
      </v-container>
    </v-main>
  </v-app>
</template>

<script setup>
import EmployeeList from '@/components/EmployeeList.vue'
</script>