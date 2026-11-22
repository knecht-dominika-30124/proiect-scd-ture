<template>
  <div>
    <!-- bara de navigare între luni -->
    <div class="d-flex align-center justify-center mb-3">
      <v-btn icon="mdi-chevron-left" variant="text" @click="emit('change', addMonths(month, -1))" />
      <div class="text-h6 mx-4 month-title">{{ title }}</div>
      <v-btn icon="mdi-chevron-right" variant="text" @click="emit('change', addMonths(month, 1))" />
    </div>

    <!-- grila: 7 coloane -->
    <div class="grid">
      <div v-for="name in WEEKDAYS" :key="name" class="weekday">{{ name }}</div>

      <div
          v-for="cell in cells"
          :key="cell.iso"
          class="cell"
          :class="{ 'other-month': !cell.inMonth, today: cell.iso === todayIso }"
          @click="emit('select', cell)"
      >
        <div class="day-number">{{ cell.date.getDate() }}</div>
        <!-- aici fiecare pagină pune ce vrea (preferințe, ture...) -->
        <slot name="day" :cell="cell" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { toIso, addDays, addMonths, firstOfMonth, lastOfMonth, mondayOf } from '@/dates'

const props = defineProps({
  month: { type: Date, required: true }, // orice zi din luna afișată
})
const emit = defineEmits(['change', 'select'])

const WEEKDAYS = ['Lun', 'Mar', 'Mie', 'Joi', 'Vin', 'Sâm', 'Dum']
const todayIso = toIso(new Date())

const title = computed(() =>
    props.month.toLocaleDateString('ro-RO', { month: 'long', year: 'numeric' }),
)

// toate căsuțele din grilă: de la lunea dinaintea zilei de 1 până la duminica de după ultima zi
const cells = computed(() => {
  const first = firstOfMonth(props.month)
  const last = lastOfMonth(props.month)
  const result = []
  let day = mondayOf(first)
  while (day <= last || result.length % 7 !== 0) {
    result.push({ date: day, iso: toIso(day), inMonth: day.getMonth() === first.getMonth() })
    day = addDays(day, 1)
  }
  return result
})
</script>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}
.weekday {
  text-align: center;
  font-weight: 600;
  padding: 4px 0;
  opacity: 0.7;
}
.cell {
  min-height: 90px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  padding: 4px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cell:hover {
  background: rgba(0, 0, 0, 0.04);
}
.other-month {
  opacity: 0.35;
}
.today {
  border: 2px solid #1976d2;
}
.day-number {
  font-weight: 600;
}
.month-title {
  text-transform: capitalize;
  min-width: 180px;
  text-align: center;
}
</style>