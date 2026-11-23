<template>
  <v-card>
    <v-card-title class="d-flex align-center flex-wrap ga-2">
      Cereri de concediu
      <v-spacer />
      <v-btn-toggle v-model="filter" mandatory density="compact" color="primary" variant="outlined">
        <v-btn value="PENDING">În așteptare</v-btn>
        <v-btn value="APPROVED">Aprobate</v-btn>
        <v-btn value="REJECTED">Respinse</v-btn>
        <v-btn value="ALL">Toate</v-btn>
      </v-btn-toggle>
    </v-card-title>

    <v-card-text>
      <v-alert v-if="error" type="error" closable class="mb-3" @click:close="error = ''">{{ error }}</v-alert>

      <v-table v-if="requests.length > 0">
        <thead>
        <tr>
          <th>Angajat</th>
          <th>Perioada</th>
          <th>Zile</th>
          <th>Motiv</th>
          <th>Status</th>
          <th>Acțiuni</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="r in requests" :key="r.id">
          <td class="font-weight-medium">{{ r.employee.name }}</td>
          <td>{{ formatDate(r.startDate) }} – {{ formatDate(r.endDate) }}</td>
          <td>{{ daysBetween(r.startDate, r.endDate) }}</td>
          <td>{{ r.reason || '—' }}</td>
          <td>
            <v-chip :color="TIME_OFF_STATUS[r.status].color" size="small" variant="flat">
              {{ TIME_OFF_STATUS[r.status].text }}
            </v-chip>
          </td>
          <td>
            <template v-if="r.status === 'PENDING'">
              <v-btn size="small" color="success" variant="tonal" class="mr-1" @click="approve(r)">Aprobă</v-btn>
              <v-btn size="small" color="error" variant="tonal" @click="reject(r)">Respinge</v-btn>
            </template>
          </td>
        </tr>
        </tbody>
      </v-table>

      <p v-else class="text-medium-emphasis">Nicio cerere {{ filter === 'PENDING' ? 'în așteptare' : '' }}.</p>
    </v-card-text>

    <v-snackbar v-model="snackbar" color="success" timeout="3500">{{ snackbarText }}</v-snackbar>
  </v-card>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { api } from '@/api'
import { formatDate, daysBetween } from '@/dates'
import { TIME_OFF_STATUS } from '@/labels'

const filter = ref('PENDING')
const requests = ref([])
const error = ref('')

const snackbar = ref(false)
const snackbarText = ref('')

async function load() {
  error.value = ''
  try {
    const params = filter.value === 'ALL' ? {} : { status: filter.value }
    const response = await api.get('/timeoff', { params })
    requests.value = response.data
  } catch (e) {
    error.value = 'Nu am putut încărca cererile. Serverul e pornit?'
  }
}

async function approve(request) {
  try {
    const response = await api.put(`/timeoff/${request.id}/approve`)
    const removed = response.data.removedFromShifts
    snackbarText.value = removed > 0
        ? `Aprobată. ${request.employee.name} a fost scos(ă) din ${removed} ${removed === 1 ? 'tură' : 'ture'}.`
        : 'Cererea a fost aprobată.'
    snackbar.value = true
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || 'Nu am putut aproba cererea.'
  }
}

async function reject(request) {
  try {
    await api.put(`/timeoff/${request.id}/reject`)
    snackbarText.value = 'Cererea a fost respinsă.'
    snackbar.value = true
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || 'Nu am putut respinge cererea.'
  }
}

// când se schimbă filtrul, reîncărcăm lista
watch(filter, load)

onMounted(load)
</script>

<template>
  <h1 class="text-h4 mb-4">Panoul managerului</h1>

  <v-tabs v-model="tab" color="primary" class="mb-4">
    <v-tab value="schedule" prepend-icon="mdi-calendar-week">Program</v-tab>
    <v-tab value="timeoff" prepend-icon="mdi-beach">Concedii</v-tab>
    <v-tab value="employees" prepend-icon="mdi-account-group">Angajați</v-tab>
  </v-tabs>

  <v-window v-model="tab">
    <v-window-item value="schedule">
      <ManagerSchedule />
    </v-window-item>
    <v-window-item value="timeoff">
      <ManagerTimeOff />
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
import EmployeeList from '@/components/EmployeeList.vue'

const tab = ref('schedule')
</script>