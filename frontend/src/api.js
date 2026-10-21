// Thin client for the SplitCloud REST API.
// Paths are relative ("/api/..."), so the same build works locally behind the
// Vite proxy and in the cloud behind CloudFront: same origin, no CORS.

async function request(path, { method = 'GET', body } = {}) {
  const res = await fetch(`/api${path}`, {
    method,
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (res.status === 204) return null

  const text = await res.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    // A non-JSON body usually means the request never reached the API (e.g. a 502 from CloudFront)
  }
  if (!res.ok) {
    throw new Error(data?.message ?? `The server could not be reached (HTTP ${res.status}). Try again in a minute.`)
  }
  return data
}

export const api = {
  listGroups: () => request('/groups'),
  createGroup: (name) => request('/groups', { method: 'POST', body: { name } }),
  createDemoGroup: () => request('/demo', { method: 'POST' }),
  getGroup: (id) => request(`/groups/${id}`),
  addMember: (id, member) => request(`/groups/${id}/members`, { method: 'POST', body: member }),
  listExpenses: (id) => request(`/groups/${id}/expenses`),
  addExpense: (id, expense) => request(`/groups/${id}/expenses`, { method: 'POST', body: expense }),
  deleteExpense: (id, expenseId) => request(`/groups/${id}/expenses/${expenseId}`, { method: 'DELETE' }),
  balances: (id) => request(`/groups/${id}/balances`),
  settlementPlan: (id) => request(`/groups/${id}/settlement-plan`),
  listSettlements: (id) => request(`/groups/${id}/settlements`),
  recordSettlement: (id, payment) => request(`/groups/${id}/settlements`, { method: 'POST', body: payment }),
  health: () => request('/health'),
}
