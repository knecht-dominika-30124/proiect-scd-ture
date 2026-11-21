<template>
  <v-card>
    <v-card-title class="d-flex align-center">
      Angajați
      <v-spacer />
      <v-btn variant="tonal" prepend-icon="mdi-refresh" @click="loadEmployees">
        Reîncarcă
      </v-btn>
    </v-card-title>

    <v-card-text>
      <v-alert v-if="error" type="error" class="mb-4">{{ error }}</v-alert>

      <v-data-table
          :headers="headers"
          :items="employees"
          :loading="loading"
          no-data-text="Nu există angajați"
      />
    </v-card-text>
  </v-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '@/api'

// datele componentei
const employees = ref([])
const loading = ref(false)
const error = ref('')

// coloanele tabelului: title = ce scrie în antet, key = câmpul din JSON
const headers = [
  { title: 'Nume', key: 'name' },
  { title: 'Email', key: 'email' },
  { title: 'Post', key: 'position' },
  { title: 'Rol', key: 'role' },
  { title: 'Ore max / săpt.', key: 'maxHoursPerWeek' },
]

// cere lista de la server (ca GET /employee din Postman)
async function loadEmployees() {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get('/employee')
    employees.value = response.data
  } catch (e) {
    error.value = 'Nu am putut încărca angajații. Serverul e pornit?'
  } finally {
    loading.value = false
  }
}

// când apare componenta pe ecran, încarcă datele
onMounted(loadEmployees)
</script>