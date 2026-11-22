<template>
  <v-card>
    <v-card-title>Preferințele mele de program</v-card-title>
    <v-card-subtitle>
      Spune-i managerului când poți lucra. Lasă orele goale dacă poți de la deschidere sau până la închidere.
    </v-card-subtitle>

    <v-card-text>
      <v-alert v-if="error" type="error" class="mb-4" closable @click:close="error = ''">{{ error }}</v-alert>

      <v-table>
        <thead>
        <tr>
          <th>Zi</th>
          <th>Pot lucra</th>
          <th>De la</th>
          <th>Până la</th>
          <th>Notă</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="day in week" :key="day.dayOfWeek">
          <td class="font-weight-medium">{{ day.label }}</td>
          <td>
            <v-switch v-model="day.available" color="primary" hide-details density="compact" />
          </td>
          <td>
            <v-text-field
                v-model="day.fromTime"
                type="time"
                :disabled="!day.available"
                hide-details
                density="compact"
            />
          </td>
          <td>
            <v-text-field
                v-model="day.toTime"
                type="time"
                :disabled="!day.available"
                hide-details
                density="compact"
            />
          </td>
          <td>
            <v-text-field v-model="day.note" placeholder="opțional" hide-details density="compact" />
          </td>
        </tr>
        </tbody>
      </v-table>
    </v-card-text>

    <v-card-actions>
      <v-spacer />
      <v-btn color="primary" variant="flat" :loading="saving" prepend-icon="mdi-content-save" @click="save">
        Salvează
      </v-btn>
    </v-card-actions>

    <v-snackbar v-model="saved" color="success" timeout="2500">Preferințele au fost salvate ✅</v-snackbar>
  </v-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '@/api'
import { currentUser } from '@/session'

const DAYS = [
  { key: 'MONDAY', label: 'Luni' },
  { key: 'TUESDAY', label: 'Marți' },
  { key: 'WEDNESDAY', label: 'Miercuri' },
  { key: 'THURSDAY', label: 'Joi' },
  { key: 'FRIDAY', label: 'Vineri' },
  { key: 'SATURDAY', label: 'Sâmbătă' },
  { key: 'SUNDAY', label: 'Duminică' },
]

// câte un rând pentru fiecare zi; id = null înseamnă că ziua nu e încă salvată pe server
const week = ref(DAYS.map(d => ({
  id: null,
  dayOfWeek: d.key,
  label: d.label,
  available: true,
  fromTime: '',
  toTime: '',
  note: '',
})))

const saving = ref(false)
const saved = ref(false)
const error = ref('')

// serverul trimite orele ca "12:00:00"; câmpul de tip time vrea "12:00"
function shortTime(time) {
  return time ? time.slice(0, 5) : ''
}

async function load() {
  try {
    const response = await api.get(`/availability/employee/${currentUser.value.id}`)
    for (const pref of response.data) {
      const day = week.value.find(d => d.dayOfWeek === pref.dayOfWeek)
      if (day) {
        day.id = pref.id
        day.available = pref.available
        day.fromTime = shortTime(pref.fromTime)
        day.toTime = shortTime(pref.toTime)
        day.note = pref.note || ''
      }
    }
  } catch (e) {
    error.value = 'Nu am putut încărca preferințele. Serverul e pornit?'
  }
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    for (const day of week.value) {
      const body = {
        employee: { id: currentUser.value.id },
        dayOfWeek: day.dayOfWeek,
        available: day.available,
        fromTime: day.available && day.fromTime ? day.fromTime : null,
        toTime: day.available && day.toTime ? day.toTime : null,
        note: day.note || null,
      }
      if (day.id) {
        await api.put(`/availability/${day.id}`, body)       // ziua exista deja → modificăm
      } else {
        const response = await api.post('/availability', body) // zi nouă → creăm
        day.id = response.data.id
      }
    }
    saved.value = true
  } catch (e) {
    error.value = e.response?.data?.message || 'Nu am putut salva preferințele.'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>