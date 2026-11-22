<template>
  <v-card>
    <!-- ===== bara de sus: navigare + status + publicare ===== -->
    <v-card-title class="d-flex align-center flex-wrap ga-2">
      <v-btn icon="mdi-chevron-left" variant="text" @click="changeWeek(-7)" />
      <span class="text-h6">{{ weekTitle }}</span>
      <v-btn icon="mdi-chevron-right" variant="text" @click="changeWeek(7)" />

      <v-spacer />

      <template v-if="schedule">
        <v-chip :color="schedule.status === 'PUBLISHED' ? 'success' : 'grey'" variant="flat">
          {{ schedule.status === 'PUBLISHED' ? 'Publicat' : 'Ciornă' }}
        </v-chip>
        <v-btn
            v-if="schedule.status === 'DRAFT'"
            color="success"
            variant="flat"
            prepend-icon="mdi-send"
            :loading="publishing"
            @click="publish"
        >
          Publică
        </v-btn>
      </template>
    </v-card-title>

    <v-card-text>
      <v-alert v-if="error" type="error" closable class="mb-3" @click:close="error = ''">{{ error }}</v-alert>

      <!-- se încarcă -->
      <div v-if="loading" class="text-center pa-6">
        <v-progress-circular indeterminate color="primary" />
      </div>

      <!-- nu există program pentru săptămâna asta -->
      <div v-else-if="!schedule" class="text-center pa-6">
        <p class="mb-4">Nu există încă un program pentru această săptămână.</p>
        <v-btn color="primary" prepend-icon="mdi-plus" @click="createSchedule">Creează programul</v-btn>
      </div>

      <!-- ===== săptămâna: 7 coloane ===== -->
      <div v-else class="week-scroll">
        <div class="week">
          <div v-for="day in days" :key="day.iso" class="day" :class="{ today: day.iso === todayIso }">
            <div class="day-header">
              <div class="font-weight-bold text-capitalize">{{ day.weekday }}</div>
              <div class="text-caption">{{ day.label }}</div>
            </div>

            <v-card
                v-for="shift in shiftsByDate[day.iso] || []"
                :key="shift.id"
                class="mb-2"
                variant="tonal"
                :color="isFull(shift) ? 'success' : 'error'"
                @click="openShift(shift)"
            >
              <v-card-text class="pa-2">
                <div class="font-weight-bold">{{ shortTime(shift.startTime) }}–{{ shortTime(shift.endTime) }}</div>
                <div v-if="shift.label" class="text-caption">{{ shift.label }}</div>
                <div class="text-caption mb-1">
                  {{ assignedTo(shift).length }}/{{ shift.requiredStaff }} oameni
                </div>
                <div v-for="a in assignedTo(shift)" :key="a.id" class="text-body-2">• {{ a.employee.name }}</div>
              </v-card-text>
            </v-card>

            <v-btn block variant="text" size="small" prepend-icon="mdi-plus" @click="openNewShift(day)">
              Tură
            </v-btn>
          </div>
        </div>
      </div>
    </v-card-text>

    <!-- ===== fereastra: tură nouă ===== -->
    <v-dialog v-model="newShiftDialog" max-width="420">
      <v-card v-if="newShiftDay">
        <v-card-title class="text-capitalize">Tură nouă: {{ newShiftDay.weekday }}, {{ newShiftDay.label }}</v-card-title>
        <v-card-text>
          <v-alert v-if="dialogError" type="error" density="compact" class="mb-3">{{ dialogError }}</v-alert>
          <div class="d-flex ga-2">
            <v-text-field v-model="newShift.startTime" type="time" label="De la" />
            <v-text-field v-model="newShift.endTime" type="time" label="Până la" />
          </div>
          <v-text-field v-model.number="newShift.requiredStaff" type="number" min="1" label="Câți oameni trebuie" />
          <v-text-field v-model="newShift.label" label="Etichetă (opțional)" placeholder="ex: Dimineață" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="newShiftDialog = false">Anulează</v-btn>
          <v-btn color="primary" variant="flat" :loading="saving" @click="saveNewShift">Adaugă tura</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- ===== fereastra: detaliile unei ture ===== -->
    <v-dialog v-model="shiftDialog" max-width="520">
      <v-card v-if="selectedShift">
        <v-card-title>
          <span class="text-capitalize">{{ formatLong(fromIso(selectedShift.date)) }}</span>,
          {{ shortTime(selectedShift.startTime) }}–{{ shortTime(selectedShift.endTime) }}
        </v-card-title>
        <v-card-subtitle>
          {{ selectedShift.label || 'Tură' }} · {{ assignedTo(selectedShift).length }}/{{ selectedShift.requiredStaff }} oameni
        </v-card-subtitle>

        <v-card-text>
          <v-alert v-if="dialogError" type="error" density="compact" class="mb-3">{{ dialogError }}</v-alert>

          <!-- cine e deja în tură -->
          <div class="text-subtitle-2 mb-1">În tură</div>
          <v-list density="compact" class="mb-3">
            <v-list-item v-for="a in assignedTo(selectedShift)" :key="a.id" :title="a.employee.name" :subtitle="a.employee.position">
              <template #append>
                <v-btn icon="mdi-close" size="small" variant="text" title="Scoate din tură" @click="removeAssignment(a)" />
              </template>
            </v-list-item>
            <v-list-item v-if="assignedTo(selectedShift).length === 0" title="Nimeni încă" />
          </v-list>

          <!-- cine poate lucra (doar dacă mai e loc) -->
          <template v-if="!isFull(selectedShift)">
            <div class="text-subtitle-2 mb-1">Pot lucra conform preferințelor</div>
            <v-list density="compact">
              <v-list-item v-for="emp in candidates" :key="emp.id" :title="emp.name" :subtitle="emp.position">
                <template #append>
                  <v-btn size="small" color="primary" variant="tonal" prepend-icon="mdi-plus" @click="addEmployee(emp)">
                    Adaugă
                  </v-btn>
                </template>
              </v-list-item>
              <v-list-item v-if="candidates.length === 0" title="Nimeni disponibil pentru această tură" />
            </v-list>
          </template>
        </v-card-text>

        <v-card-actions>
          <v-btn color="error" variant="text" prepend-icon="mdi-delete" @click="deleteShift">Șterge tura</v-btn>
          <v-spacer />
          <v-btn variant="text" @click="shiftDialog = false">Închide</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="snackbar" color="success" timeout="2500">{{ snackbarText }}</v-snackbar>
  </v-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '@/api'
import { toIso, fromIso, addDays, mondayOf, formatLong, shortTime } from '@/dates'

const todayIso = toIso(new Date())

// ---------- săptămâna afișată ----------

const weekStart = ref(mondayOf(new Date()))

const days = computed(() => {
  const result = []
  for (let i = 0; i < 7; i++) {
    const date = addDays(weekStart.value, i)
    result.push({
      date,
      iso: toIso(date),
      weekday: date.toLocaleDateString('ro-RO', { weekday: 'long' }),
      label: date.toLocaleDateString('ro-RO', { day: 'numeric', month: 'long' }),
    })
  }
  return result
})

const weekTitle = computed(() => {
  const first = days.value[0]
  const last = days.value[6]
  return `${first.label} – ${last.label} ${last.date.getFullYear()}`
})

function changeWeek(daysToAdd) {
  weekStart.value = addDays(weekStart.value, daysToAdd)
  load()
}

// ---------- datele de pe server ----------

const schedule = ref(null)
const shifts = ref([])
const assignments = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  schedule.value = null
  shifts.value = []
  assignments.value = []
  try {
    const response = await api.get('/schedule/week', { params: { date: toIso(weekStart.value) } })
    schedule.value = response.data
    await loadShifts()
  } catch (e) {
    // 404 = nu există program pentru săptămâna asta, deci nu e o eroare reală
    if (e.response?.status !== 404) {
      error.value = 'Nu am putut încărca programul. Serverul e pornit?'
    }
  } finally {
    loading.value = false
  }
}

// turele și alocările săptămânii, cerute în paralel
async function loadShifts() {
  const [shiftsResponse, assignmentsResponse] = await Promise.all([
    api.get(`/shift/schedule/${schedule.value.id}`),
    api.get(`/assignment/schedule/${schedule.value.id}`),
  ])
  shifts.value = shiftsResponse.data
  assignments.value = assignmentsResponse.data
}

// { "2026-11-30": [tura1, tura2], ... }
const shiftsByDate = computed(() => {
  const map = {}
  for (const shift of shifts.value) {
    if (!map[shift.date]) map[shift.date] = []
    map[shift.date].push(shift)
  }
  return map
})

function assignedTo(shift) {
  return assignments.value.filter(a => a.shift.id === shift.id)
}

function isFull(shift) {
  return assignedTo(shift).length >= shift.requiredStaff
}

// ---------- program nou / publicare ----------

async function createSchedule() {
  try {
    await api.post('/schedule', { weekStart: toIso(weekStart.value) })
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || 'Nu am putut crea programul.'
  }
}

const publishing = ref(false)

async function publish() {
  publishing.value = true
  try {
    const response = await api.put(`/schedule/${schedule.value.id}/publish`)
    schedule.value = response.data
    showMessage('Programul a fost publicat. Angajații au primit notificări. 📤')
  } catch (e) {
    error.value = e.response?.data?.message || 'Nu am putut publica programul.'
  } finally {
    publishing.value = false
  }
}

// ---------- tură nouă ----------

const newShiftDialog = ref(false)
const newShiftDay = ref(null)
const newShift = ref({})
const saving = ref(false)
const dialogError = ref('')

function openNewShift(day) {
  newShiftDay.value = day
  newShift.value = { startTime: '08:00', endTime: '15:00', requiredStaff: 2, label: '' }
  dialogError.value = ''
  newShiftDialog.value = true
}

async function saveNewShift() {
  saving.value = true
  dialogError.value = ''
  try {
    await api.post('/shift', {
      schedule: { id: schedule.value.id },
      date: newShiftDay.value.iso,
      startTime: newShift.value.startTime,
      endTime: newShift.value.endTime,
      requiredStaff: newShift.value.requiredStaff,
      label: newShift.value.label || null,
    })
    newShiftDialog.value = false
    await loadShifts()
  } catch (e) {
    dialogError.value = e.response?.data?.message || 'Nu am putut adăuga tura.'
  } finally {
    saving.value = false
  }
}

// ---------- detaliile unei ture ----------

const shiftDialog = ref(false)
const selectedShiftId = ref(null)
const candidates = ref([])

// ținem minte doar id-ul, ca tura să fie mereu cea „proaspătă” după reîncărcare
const selectedShift = computed(() => shifts.value.find(s => s.id === selectedShiftId.value))

async function openShift(shift) {
  selectedShiftId.value = shift.id
  dialogError.value = ''
  shiftDialog.value = true
  await loadCandidates()
}

async function loadCandidates() {
  const response = await api.get(`/assignment/shift/${selectedShiftId.value}/available`)
  candidates.value = response.data
}

async function addEmployee(employee) {
  dialogError.value = ''
  try {
    await api.post('/assignment', { shiftId: selectedShiftId.value, employeeId: employee.id })
    await loadShifts()
    await loadCandidates()
  } catch (e) {
    // aici apar regulile de pe server: „ar depăși limita de ore”, „lucrează deja...”
    dialogError.value = e.response?.data?.message || 'Nu am putut adăuga angajatul.'
  }
}

async function removeAssignment(assignment) {
  dialogError.value = ''
  try {
    await api.delete(`/assignment/${assignment.id}`)
    await loadShifts()
    await loadCandidates()
  } catch (e) {
    dialogError.value = e.response?.data?.message || 'Nu am putut scoate angajatul.'
  }
}

async function deleteShift() {
  try {
    await api.delete(`/shift/${selectedShiftId.value}`)
    shiftDialog.value = false
    await loadShifts()
  } catch (e) {
    dialogError.value = e.response?.data?.message || 'Nu am putut șterge tura.'
  }
}

// ---------- mesajul verde de jos ----------

const snackbar = ref(false)
const snackbarText = ref('')

function showMessage(text) {
  snackbarText.value = text
  snackbar.value = true
}

onMounted(load)
</script>

<style scoped>
.week-scroll {
  overflow-x: auto;
}
.week {
  display: grid;
  grid-template-columns: repeat(7, minmax(140px, 1fr));
  gap: 8px;
}
.day {
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  padding: 6px;
  min-height: 220px;
}
.day.today {
  border: 2px solid #1976d2;
}
.day-header {
  text-align: center;
  margin-bottom: 8px;
}
</style>