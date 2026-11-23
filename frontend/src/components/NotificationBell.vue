<template>
  <v-menu v-model="open" :close-on-content-click="false" location="bottom end">
    <!-- butonul cu clopoțel -->
    <template #activator="{ props }">
      <v-btn icon v-bind="props" class="mr-2" title="Notificări">
        <v-badge :content="unreadCount" :model-value="unreadCount > 0" color="error">
          <v-icon>mdi-bell</v-icon>
        </v-badge>
      </v-btn>
    </template>

    <!-- lista care se deschide -->
    <v-card width="380">
      <v-card-title class="d-flex align-center">
        Notificări
        <v-spacer />
        <v-btn v-if="unreadCount > 0" size="small" variant="text" @click="markAllSeen">Marchează citite</v-btn>
      </v-card-title>

      <v-list density="compact" class="notifications-list">
        <v-list-item
            v-for="n in notifications"
            :key="n.id"
            :class="{ unread: !n.seen }"
            @click="markSeen(n)"
        >
          <template #prepend>
            <v-icon :color="n.seen ? 'grey' : 'primary'" size="small">
              {{ n.seen ? 'mdi-bell-outline' : 'mdi-bell-ring' }}
            </v-icon>
          </template>
          <div class="text-body-2">{{ n.message }}</div>
          <div class="text-caption text-medium-emphasis">{{ formatDateTime(n.createdOn) }}</div>
        </v-list-item>

        <v-list-item v-if="notifications.length === 0">
          <div class="text-medium-emphasis">Nu ai nicio notificare.</div>
        </v-list-item>
      </v-list>
    </v-card>
  </v-menu>

  <!-- mesajul care apare sus, când vine o notificare nouă -->
  <v-snackbar v-model="toast" color="primary" location="top right" timeout="5000">
    🔔 {{ toastText }}
  </v-snackbar>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { api } from '@/api'
import { currentUser } from '@/session'

const open = ref(false)
const notifications = ref([])
const toast = ref(false)
const toastText = ref('')

const unreadCount = computed(() => notifications.value.filter(n => !n.seen).length)

function formatDateTime(value) {
  return new Date(value).toLocaleString('ro-RO', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// ---------- notificările vechi (salvate în baza de date) ----------

async function load() {
  try {
    const response = await api.get(`/notification/employee/${currentUser.value.id}`)
    notifications.value = response.data
  } catch (e) {
    // dacă serverul e oprit, clopoțelul rămâne gol, nu stricăm pagina
  }
}

async function markSeen(notification) {
  if (notification.seen) return
  await api.put(`/notification/${notification.id}/seen`)
  notification.seen = true
}

async function markAllSeen() {
  const unread = notifications.value.filter(n => !n.seen)
  await Promise.all(unread.map(n => api.put(`/notification/${n.id}/seen`)))
  unread.forEach(n => (n.seen = true))
}

// ---------- notificările noi, în timp real (SSE) ----------

let eventSource = null

function connect() {
  const url = `${api.defaults.baseURL}/notification/stream/${currentUser.value.id}`
  eventSource = new EventSource(url)

  eventSource.addEventListener('notification', (event) => {
    const notification = JSON.parse(event.data)
    notifications.value.unshift(notification) // o punem prima în listă
    toastText.value = notification.message
    toast.value = true
  })
}

onMounted(() => {
  load()
  connect()
})

// când componenta dispare (ex: la Ieșire), închidem conexiunea
onUnmounted(() => {
  eventSource?.close()
})
</script>

<style scoped>
.notifications-list {
  max-height: 380px;
  overflow-y: auto;
}
.unread {
  background: rgba(25, 118, 210, 0.08);
}
</style>