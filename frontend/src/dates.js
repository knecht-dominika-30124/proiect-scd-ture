// Funcții ajutătoare pentru lucrul cu date.

// Date → "2026-12-01" (formatul pe care îl înțelege serverul)
export function toIso(date) {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
}

// "2026-12-01" → Date
export function fromIso(iso) {
    const [y, m, d] = iso.split('-').map(Number)
    return new Date(y, m - 1, d)
}

export function addDays(date, days) {
    const result = new Date(date)
    result.setDate(result.getDate() + days)
    return result
}

export function addMonths(date, months) {
    return new Date(date.getFullYear(), date.getMonth() + months, 1)
}

export function firstOfMonth(date) {
    return new Date(date.getFullYear(), date.getMonth(), 1)
}

export function lastOfMonth(date) {
    return new Date(date.getFullYear(), date.getMonth() + 1, 0)
}

// lunea săptămânii în care se află data
export function mondayOf(date) {
    const result = new Date(date)
    const daysSinceMonday = (result.getDay() + 6) % 7
    result.setDate(result.getDate() - daysSinceMonday)
    return result
}

// Date → "marți, 1 decembrie"
export function formatLong(date) {
    return date.toLocaleDateString('ro-RO', { weekday: 'long', day: 'numeric', month: 'long' })
}

// "12:00:00" → "12:00"
export function shortTime(time) {
    return time ? time.slice(0, 5) : ''

}

// "2026-12-01" → "1 dec. 2026"
export function formatDate(iso) {
    return fromIso(iso).toLocaleDateString('ro-RO', { day: 'numeric', month: 'short', year: 'numeric' })
}

// câte zile are intervalul, inclusiv capetele: 15–17 decembrie = 3 zile
export function daysBetween(fromIsoDate, toIsoDate) {
    const ms = fromIso(toIsoDate) - fromIso(fromIsoDate)
    return Math.round(ms / (24 * 60 * 60 * 1000)) + 1
}