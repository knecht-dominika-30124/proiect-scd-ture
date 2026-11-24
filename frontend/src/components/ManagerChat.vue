<template>
  <v-card>
    <v-card-title class="d-flex align-center flex-wrap ga-2">
      <v-icon color="primary">mdi-robot-happy-outline</v-icon>
      Asistent AI
      <v-spacer />
      <v-btn variant="text" prepend-icon="mdi-broom" :disabled="thinking" @click="clearConversation">
        Conversație nouă
      </v-btn>
    </v-card-title>

    <v-card-subtitle class="d-flex align-center">
      Despre săptămâna:
      <v-btn icon="mdi-chevron-left" size="small" variant="text" @click="changeWeek(-7)" />
      <strong>{{ weekTitle }}</strong>
      <v-btn icon="mdi-chevron-right" size="small" variant="text" @click="changeWeek(7)" />
    </v-card-subtitle>

    <v-card-text>
      <!-- ===== mesajele ===== -->
      <div ref="messagesBox" class="messages">
        <div v-if="messages.length === 0 && !thinking" class="text-center text-medium-emphasis pa-6">
          Întreabă-mă orice despre program, angajați sau concedii. 🙂
        </div>

        <div
            v-for="(m, index) in messages"
            :key="index"
            class="message"
            :class="m.role === 'USER' ? 'from-user' : 'from-ai'"
        >
          <div class="bubble" v-html="renderText(m.content)"></div>
        </div>

        <div v-if="thinking" class="message from-ai">
          <div class="bubble text-medium-emphasis">
            <v-progress-circular indeterminate size="16" width="2" class="mr-2" />
            AI-ul se gândește...
          </div>
        </div>
      </div>

      <!-- ===== întrebări rapide ===== -->
      <div class="d-flex flex-wrap ga-2 my-3">
        <v-chip
            v-for="q in QUICK_QUESTIONS"
            :key="q"
            size="small"
            variant="outlined"
            color="primary"
            :disabled="thinking"
            @click="send(q)"
        >
          {{ q }}
        </v-chip>
      </div>

      <!-- ===== căsuța de scris ===== -->
      <div class="d-flex ga-2 align-end">
        <v-textarea
            v-model="question"
            placeholder="Scrie o întrebare... (Enter = trimite, Shift+Enter = rând nou)"
            rows="1"
            auto-grow
            max-rows="4"
            hide-details
            variant="outlined"
            density="comfortable"
            :disabled="thinking"
            @keydown.enter.exact.prevent="send()"
        />
        <v-btn color="primary" icon="mdi-send" :loading="thinking" :disabled="!question.trim()" @click="send()" />
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { api } from '@/api'
import { currentUser } from '@/session'
import { toIso, addDays, mondayOf } from '@/dates'

const QUICK_QUESTIONS = [
  'Ce ture sunt neacoperite săptămâna asta?',
  'Pe cine îmi recomanzi pentru turele libere?',
  'Ce cereri de concediu am de aprobat?',
  'Cine are cele mai multe ore săptămâna asta?',
]

// ---------- săptămâna despre care întrebăm ----------

const weekStart = ref(mondayOf(new Date()))

const weekTitle = computed(() => {
  const end = addDays(weekStart.value, 6)
  const options = { day: 'numeric', month: 'long' }
  return `${weekStart.value.toLocaleDateString('ro-RO', options)} – ${end.toLocaleDateString('ro-RO', options)}`
})

function changeWeek(daysToAdd) {
  weekStart.value = addDays(weekStart.value, daysToAdd)
}

// ---------- conversația ----------

const messages = ref([])        // [{ role: 'USER' | 'ASSISTANT', content: '...' }]
const question = ref('')
const thinking = ref(false)
const messagesBox = ref(null)   // legătura cu <div ref="messagesBox"> din template

async function loadHistory() {
  try {
    const response = await api.get(`/chat/${currentUser.value.id}/history`)
    messages.value = response.data.map(m => ({ role: m.role, content: m.content }))
    scrollToBottom()
  } catch (e) {
    // fără istoric, începem o conversație goală
  }
}

async function send(text) {
  const message = (text ?? question.value).trim()
  if (!message || thinking.value) return

  question.value = ''
  messages.value.push({ role: 'USER', content: message }) // apare imediat în chat
  thinking.value = true
  scrollToBottom()

  try {
    const response = await api.post('/chat', {
      managerId: currentUser.value.id,
      message,
      week: toIso(weekStart.value),
    })
    messages.value.push({ role: 'ASSISTANT', content: response.data.reply })
  } catch (e) {
    messages.value.push({
      role: 'ASSISTANT',
      content: '⚠️ ' + (e.response?.data?.message || 'Nu am putut contacta serverul.'),
    })
  } finally {
    thinking.value = false
    scrollToBottom()
  }
}

async function clearConversation() {
  await api.delete(`/chat/${currentUser.value.id}/history`)
  messages.value = []
}

// derulează lista de mesaje până jos, la ultimul mesaj
async function scrollToBottom() {
  await nextTick() // așteptăm ca Vue să deseneze întâi mesajul nou
  const box = messagesBox.value
  if (box) {
    box.scrollTop = box.scrollHeight
  }
}

// AI-ul scrie uneori **text îngroșat**; îl afișăm frumos, dar în siguranță
function renderText(text) {
  const safe = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
  return safe.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
}

onMounted(loadHistory)
</script>

<style scoped>
.messages {
  height: 420px;
  overflow-y: auto;
  padding: 12px;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 8px;
}
.message {
  display: flex;
  margin-bottom: 10px;
}
.from-user {
  justify-content: flex-end;
}
.bubble {
  max-width: 80%;
  padding: 8px 12px;
  border-radius: 14px;
  white-space: pre-wrap;
  line-height: 1.45;
}
.from-user .bubble {
  background: #1976d2;
  color: white;
  border-bottom-right-radius: 4px;
}
.from-ai .bubble {
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-bottom-left-radius: 4px;
}
</style>