<template>
  <v-row>
    <!-- ===== formularul ===== -->
    <v-col cols="12" md="5">
      <v-card>
        <v-card-title>Cere liber</v-card-title>
        <v-card-text>
          <v-alert v-if="formError" type="error" density="compact" class="mb-3">{{ formError }}</v-alert>

          <v-text-field v-model="form.startDate" type="date" label="De la" :min="todayIso" />
          <v-text-field v-model="form.endDate" type="date" label="Până la" :min="form.startDate || todayIso" />
          <v-text-field v-model="form.reason" label="Motiv (opțional)" placeholder="ex: vacanță, medic" />

          <div v-if="form.startDate && form.endDate && form.endDate >= form.startDate" class="text-body-2">
            Total: <strong>{{ daysBetween(form.startDate, form.endDate) }}</strong> zile
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn
              color="primary"
              variant="flat"
              prepend-icon="mdi-send"
              :disabled="!form.startDate || !form.endDate"
              :loading="sending"
              @click="send"
          >
            Trimite cererea
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-col>

    <!-- ===== lista cererilor ===== -->
    <v-col cols="12" md="7">
      <v-card>
        <v-card-title>Cererile mele</v-card-title>
        <v-card-text>
          <v-alert v-if="loadError" type="error" class="mb-3">{{ loadError }}</v-alert>

          <v-table v-if="requests.length > 0" density="comfortable">
            <thead>
            <tr>
              <th>Perioada</th>
              <th>Zile</th>
              <th>Motiv</th>
              <th>Status</th>
              <th></th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="r in requests" :key="r.id">
              <td>{{ formatDate(r.startDate) }} – {{ formatDate(r.endDate) }}</td>
              <td>{{ daysBetween(r.startDate, r.endDate) }}</td>
              <td>{{ r.reason || '—' }}</td>
              <td>
                <v-chip
                    :color="TIME_OFF_STATUS[r.status].color"
                    :prepend-icon="TIME_OFF_STATUS[r.status].icon"
                    size="small"
                    variant="flat"
                >
                  {{ TIME_OFF_STATUS[r.status].text }}
                </v-chip>
              </td>
              <td>
                <v-btn v-if="r.status === 'PENDING'" size="small" variant="text" color="error" @click="withdraw(r)">
                  Retrage
                </v-btn>
              </td>
            </tr>
            </tbody>
          </v-table>

          <p v-else class="text-medium-emphasis">Nu ai trimis nicio cerere.</p>
        </v-card-text>
      </v-card>
    </v-col>
  </v-row>

  <v-snackbar v-model="snackbar" color="success" timeout="2500">{{ snackbarText }}</v-snackbar>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '@/api'
import { currentUser } from '@/session'
import { toIso, formatDate, daysBetween } from '@/dates'
import { TIME_OFF_STATUS } from '@/labels'

const todayIso = toIso(new Date())

const requests = ref([])
const loadError = ref('')

const form = ref({ startDate: '', endDate: '', reason: '' })
const formError = ref('')
const sending = ref(false)

const snackbar = ref(false)
const snackbarText = ref('')

async function load() {
  loadError.value = ''
  try {
    const response = await api.get(`/timeoff/employee/${currentUser.value.id}`)
    requests.value = response.data
  } catch (e) {
    loadError.value = 'Nu am putut încărca cererile. Serverul e pornit?'
  }
}

async function send() {
  formError.value = ''
  sending.value = true
  try {
    await api.post('/timeoff', {
      employee: { id: currentUser.value.id },
      startDate: form.value.startDate,
      endDate: form.value.endDate,
      reason: form.value.reason || null,
    })
    form.value = { startDate: '', endDate: '', reason: '' } // golim formularul
    snackbarText.value = 'Cererea a fost trimisă managerului ✅'
    snackbar.value = true
    await load()
  } catch (e) {
    formError.value = e.response?.data?.message || 'Nu am putut trimite cererea.'
  } finally {
    sending.value = false
  }
}

async function withdraw(request) {
  try {
    await api.delete(`/timeoff/${request.id}`)
    snackbarText.value = 'Cererea a fost retrasă'
    snackbar.value = true
    await load()
  } catch (e) {
    loadError.value = e.response?.data?.message || 'Nu am putut retrage cererea.'
  }
}

onMounted(load)
</script>

<template>
  <h1 class="text-h4 mb-4">Bună, {{ currentUser.name }}! 👋</h1>

  <v-tabs v-model="tab" color="primary" class="mb-4">
    <v-tab value="shifts" prepend-icon="mdi-calendar-clock">Turele mele</v-tab>
    <v-tab value="preferences" prepend-icon="mdi-calendar-edit">Preferințe</v-tab>
    <v-tab value="timeoff" prepend-icon="mdi-beach">Concediu</v-tab>
  </v-tabs>

  <v-window v-model="tab">
    <v-window-item value="shifts">
      <MyShiftsCalendar />
    </v-window-item>
    <v-window-item value="preferences">
      <PreferencesCalendar />
    </v-window-item>
    <v-window-item value="timeoff">
      <StaffTimeOff />
    </v-window-item>
  </v-window>
</template>

<script setup>
import { ref } from 'vue'
import { currentUser } from '@/session'
import MyShiftsCalendar from '@/components/MyShiftsCalendar.vue'
import PreferencesCalendar from '@/components/PreferencesCalendar.vue'
import StaffTimeOff from '@/components/StaffTimeOff.vue'

const tab = ref('shifts')
</script>