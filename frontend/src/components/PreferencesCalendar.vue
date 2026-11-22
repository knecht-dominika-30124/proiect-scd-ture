<template>
  <v-card>
    <v-card-title>Preferințele mele</v-card-title>
    <v-card-subtitle>
      Apasă pe o zi ca să ceri zi liberă sau un interval orar. Zilele nemarcate = poți lucra oricând.
    </v-card-subtitle>

    <v-card-text>
      <div class="d-flex flex-wrap ga-2 mb-3">
        <v-chip color="red" variant="tonal">
          Zile libere în {{ monthName }}: {{ daysOffCount }} / {{ MAX_DAYS_OFF }}
        </v-chip>
        <v-chip color="amber-darken-3" variant="tonal">
          Restricții de oră: maxim {{ MAX_HOUR_LIMITS }} pe săptămână
        </v-chip>
      </div>

      <v-alert v-if="loadError" type="error" class="mb-3">{{ loadError }}</v-alert>

      <MonthGrid :month="month" @change="changeMonth" @select="openDay">
        <template #day="{ cell }">
          <template v-if="cell.inMonth && prefByDate[cell.iso]">
            <v-chip v-if="!prefByDate[cell.iso].available" size="small" color="red" variant="flat">
              Liber
            </v-chip>
            <v-chip v-else size="small" color="amber-darken-3" variant="flat">
              {{ describeHours(prefByDate[cell.iso]) }}
            </v-chip>
          </template>
        </template>
      </MonthGrid>
    </v-card-text>

    <!-- fereastra care se deschide la click pe o zi -->
    <v-dialog v-model="dialog" max-width="440">
      <v-card v-if="selected">
        <v-card-title class="text-capitalize">{{ formatLong(selected.date) }}</v-card-title>

        <v-card-text>
          <v-alert v-if="saveError" type="error" density="compact" class="mb-3">{{ saveError }}</v-alert>

          <v-radio-group v-model="form.choice" hide-details>
            <v-radio label="Pot lucra oricând" value="any" />
            <v-radio label="Zi liberă" value="off" />
            <v-radio label="Pot lucra doar într-un interval" value="hours" />
          </v-radio-group>

          <div v-if="form.choice === 'hours'" class="d-flex ga-2 mt-3">
            <v-text-field v-model="form.fromTime" type="time" label="De la" hint="gol = de la deschidere" persistent-hint />
            <v-text-field v-model="form.toTime" type="time" label="Până la" hint="gol = până la închidere" persistent-hint />
          </div>

          <v-text-field
              v-if="form.choice !== 'any'"
              v-model="form.note"
              label="Motiv (opțional)"
              class="mt-3"
          />
        </v-card-text>

        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialog = false">Anulează</v-btn>
          <v-btn color="primary" variant="flat" :loading="saving" @click="save">Salvează</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="saved" color="success" timeout="2000">Salvat ✅</v-snackbar>
  </v-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '@/api'
import { currentUser } from '@/session'
import MonthGrid from '@/components/MonthGrid.vue'
import { toIso, firstOfMonth, lastOfMonth, formatLong, shortTime } from '@/dates'

// aceleași limite ca pe server (AvailabilityService)
const MAX_DAYS_OFF = 4
const MAX_HOUR_LIMITS = 2

const month = ref(firstOfMonth(new Date()))
const prefs = ref([])
const loadError = ref('')
const todayIso = toIso(new Date())

// ---------- valori calculate ----------

// { "2026-12-03": {...preferința}, ... }, ca să găsim rapid preferința unei zile
const prefByDate = computed(() => {
  const map = {}
  for (const p of prefs.value) {
    map[p.date] = p
  }
  return map
})

const daysOffCount = computed(() => prefs.value.filter(p => !p.available).length)

const monthName = computed(() => month.value.toLocaleDateString('ro-RO', { month: 'long' }))

function describeHours(p) {
  const from = shortTime(p.fromTime)
  const to = shortTime(p.toTime)
  if (from && to) return `${from}–${to}`
  if (from) return `de la ${from}`
  return `până la ${to}`
}

// ---------- încărcare ----------

async function load() {
  loadError.value = ''
  try {
    const response = await api.get(`/availability/employee/${currentUser.value.id}`, {
      params: { from: toIso(firstOfMonth(month.value)), to: toIso(lastOfMonth(month.value)) },
    })
    prefs.value = response.data
  } catch (e) {
    loadError.value = 'Nu am putut încărca preferințele. Serverul e pornit?'
  }
}

function changeMonth(newMonth) {
  month.value = newMonth
  load()
}

// ---------- fereastra de editare ----------

const dialog = ref(false)
const selected = ref(null)
const form = ref({ choice: 'any', fromTime: '', toTime: '', note: '' })
const saving = ref(false)
const saveError = ref('')
const saved = ref(false)

function openDay(cell) {
  if (!cell.inMonth) return       // zilele din luna vecină: schimbă luna din săgeți
  if (cell.iso < todayIso) return  // trecutul nu se mai modifică

  selected.value = cell
  const existing = prefByDate.value[cell.iso]
  form.value = {
    choice: !existing ? 'any' : existing.available ? 'hours' : 'off',
    fromTime: shortTime(existing?.fromTime),
    toTime: shortTime(existing?.toTime),
    note: existing?.note || '',
  }
  saveError.value = ''
  dialog.value = true
}

async function save() {
  saving.value = true
  saveError.value = ''
  const existing = prefByDate.value[selected.value.iso]

  try {
    if (form.value.choice === 'any') {
      // „pot lucra oricând” = ștergem preferința, dacă exista
      if (existing) {
        await api.delete(`/availability/${existing.id}`)
      }
    } else {
      const isHours = form.value.choice === 'hours'
      const body = {
        employee: { id: currentUser.value.id },
        date: selected.value.iso,
        available: isHours,
        fromTime: isHours && form.value.fromTime ? form.value.fromTime : null,
        toTime: isHours && form.value.toTime ? form.value.toTime : null,
        note: form.value.note || null,
      }
      if (existing) {
        await api.put(`/availability/${existing.id}`, body)
      } else {
        await api.post('/availability', body)
      }
    }
    dialog.value = false
    saved.value = true
    await load()
  } catch (e) {
    // aici apar mesajele tale de pe server: „maxim 4 zile libere...”
    saveError.value = e.response?.data?.message || 'Nu am putut salva.'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>