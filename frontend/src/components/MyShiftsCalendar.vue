<template>
  <v-card>
    <v-card-title>Turele mele</v-card-title>
    <v-card-subtitle>Apar doar programele publicate de manager.</v-card-subtitle>

    <v-card-text>
      <div class="d-flex flex-wrap ga-2 mb-3">
        <v-chip color="primary" variant="tonal" prepend-icon="mdi-calendar-check">
          Ture în {{ monthName }}: {{ shifts.length }}
        </v-chip>
        <v-chip color="primary" variant="tonal" prepend-icon="mdi-clock-outline">
          Total: {{ totalHours }} ore
        </v-chip>
      </div>

      <v-alert v-if="error" type="error" class="mb-3">{{ error }}</v-alert>

      <MonthGrid :month="month" @change="changeMonth">
        <template #day="{ cell }">
          <template v-if="cell.inMonth">
            <v-chip
                v-for="shift in shiftsByDate[cell.iso] || []"
                :key="shift.id"
                size="small"
                color="primary"
                variant="flat"
                label
                :title="shift.label"
            >
              {{ shortTime(shift.startTime) }}–{{ shortTime(shift.endTime) }}
            </v-chip>
          </template>
        </template>
      </MonthGrid>
    </v-card-text>
  </v-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '@/api'
import { currentUser } from '@/session'
import MonthGrid from '@/components/MonthGrid.vue'
import { toIso, firstOfMonth, lastOfMonth, shortTime } from '@/dates'

const month = ref(firstOfMonth(new Date()))
const shifts = ref([])
const error = ref('')

// { "2026-12-01": [tura1, tura2], ... }: o zi poate avea mai multe ture
const shiftsByDate = computed(() => {
  const map = {}
  for (const shift of shifts.value) {
    if (!map[shift.date]) {
      map[shift.date] = []
    }
    map[shift.date].push(shift)
  }
  return map
})

const totalHours = computed(() => shifts.value.reduce((sum, shift) => sum + shift.hours, 0))

const monthName = computed(() => month.value.toLocaleDateString('ro-RO', { month: 'long' }))

async function load() {
  error.value = ''
  try {
    const response = await api.get(`/assignment/employee/${currentUser.value.id}/shifts`, {
      params: { from: toIso(firstOfMonth(month.value)), to: toIso(lastOfMonth(month.value)) },
    })
    shifts.value = response.data
  } catch (e) {
    error.value = 'Nu am putut încărca turele. Serverul e pornit?'
  }
}

function changeMonth(newMonth) {
  month.value = newMonth
  load()
}

onMounted(load)
</script>