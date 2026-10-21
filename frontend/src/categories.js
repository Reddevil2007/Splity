// Expense categories and their chart colours. The keyword guess mirrors
// ExpenseCategory.guess() in the backend so the form can preview it live.

export const CATEGORIES = [
  { id: 'FOOD', label: 'Food', color: '#D64545', keywords: ['food', 'dinner', 'lunch', 'breakfast', 'meal', 'restaurant', 'shack', 'pizza', 'snack', 'grocer', 'cafe', 'coffee', 'biryani', 'swiggy', 'zomato'] },
  { id: 'DRINKS', label: 'Drinks', color: '#8E5BD6', keywords: ['drink', 'beer', 'tequila', 'vodka', 'wine', 'bar', 'pub', 'cocktail', 'whisky', 'rum'] },
  { id: 'TRAVEL', label: 'Travel', color: '#2E9E6B', keywords: ['cab', 'taxi', 'uber', 'auto', 'bus', 'train', 'flight', 'fuel', 'petrol', 'scooter', 'bike', 'rental', 'metro', 'ticket', 'toll', 'airport'] },
  { id: 'STAY', label: 'Stay', color: '#2F6FD6', keywords: ['hotel', 'stay', 'hostel', 'airbnb', 'rent', 'room', 'resort', 'booking', 'villa'] },
  { id: 'ACTIVITIES', label: 'Activities', color: '#E08A1E', keywords: ['parasail', 'ticket', 'museum', 'movie', 'trek', 'tour', 'game', 'club', 'party', 'entry', 'activity', 'scuba', 'surf'] },
  { id: 'SHOPPING', label: 'Shopping', color: '#D6458E', keywords: ['shop', 'souvenir', 'clothes', 'gift', 'market', 'mall'] },
  { id: 'OTHER', label: 'Other', color: '#7A8599', keywords: [] },
]

const BY_ID = Object.fromEntries(CATEGORIES.map((c) => [c.id, c]))

export function category(id) {
  return BY_ID[id] ?? BY_ID.OTHER
}

export function guessCategory(description) {
  const text = String(description ?? '').toLowerCase()
  return CATEGORIES.find((c) => c.keywords.some((k) => text.includes(k)))?.id ?? 'OTHER'
}

// Each distinct day gets its own colour, so entries from the same day read as a group.
const DAY_COLORS = ['#E3A008', '#2F6FD6', '#2E9E6B', '#8E5BD6', '#D64545', '#0E8F9E', '#D6458E']

/** Map of 'YYYY-MM-DD' to colour, newest day first. */
export function dayColors(dates) {
  const days = [...new Set(dates)].sort().reverse()
  return Object.fromEntries(days.map((d, i) => [d, DAY_COLORS[i % DAY_COLORS.length]]))
}

/** Today's date as 'YYYY-MM-DD' in the browser's own time zone. */
export function todayIso() {
  const now = new Date()
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 10)
}

const dayFormat = new Intl.DateTimeFormat('en-IN', { weekday: 'short', day: 'numeric', month: 'short' })

/** '2026-10-22' -> 'Thu, 22 Oct' (parsed as a calendar date, not shifted by time zone). */
export function formatDay(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return dayFormat.format(new Date(y, m - 1, d))
}
