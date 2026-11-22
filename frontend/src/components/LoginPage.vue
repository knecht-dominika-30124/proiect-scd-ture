<template>
  <v-card max-width="500" class="mx-auto mt-10">
    <v-card-title class="text-h5">Cine ești?</v-card-title>
    <v-card-subtitle>Alege-ți numele din listă</v-card-subtitle>

    <v-card-text>
      <v-alert v-if="error" type="error" class="mb-4">{{ error }}</v-alert>

      <v-select
          v-model="selected"
          :items="employees"
          item-title="name"
          :item-props="itemProps"
          return-object
          label="Angajat"
          :loading="loading"
      />
    </v-card-text>

    <v-card-actions>
      <v-spacer />
      <v-btn color="primary" variant="flat" :disabled="!selected" @click="login(selected)">
        Intră
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '@/api'
import { login } from '@/session'

const employees = ref([])
const selected = ref(null)
const loading = ref(false)
const error = ref('')

// sub nume afișăm și rolul, ca să se vadă cine e manager
function itemProps(employee) {
  return { subtitle: employee.role === 'MANAGER' ? 'Manager' : employee.position }
}

onMounted(async () => {
  loading.value = true
  try {
    const response = await api.get('/employee')
    employees.value = response.data
  } catch (e) {
    error.value = 'Nu am putut încărca angajații. Serverul e pornit?'
  } finally {
    loading.value = false
  }
})
</script>