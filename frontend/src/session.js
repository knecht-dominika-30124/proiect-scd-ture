import { ref } from 'vue'

// utilizatorul logat (sau null dacă nu e nimeni)
// îl citim din memoria browserului, ca să rămână logat după refresh
const saved = localStorage.getItem('currentUser')
export const currentUser = ref(saved ? JSON.parse(saved) : null)

export function login(employee) {
    currentUser.value = employee
    localStorage.setItem('currentUser', JSON.stringify(employee))
}

export function logout() {
    currentUser.value = null
    localStorage.removeItem('currentUser')
}