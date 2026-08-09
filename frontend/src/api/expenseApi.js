import apiClient from './apiClient'


export async function getExpenseById(id, signal) {
  const response = await apiClient.get(`/expenses/${id}`, { signal })
  return response.data.data
}

export async function createExpense(expenseData, signal) {
  const response = await apiClient.post('/expenses', expenseData, { signal })
  return response.data.data
}

export async function updateExpenseById(expenseId, expenseData, signal) {
  const response = await apiClient.patch(`/expenses/${expenseId}`, expenseData, { signal })
  return response.data.data
} 

export async function deleteExpenseById(expenseId, signal) {
  const response = await apiClient.delete(`/expenses/${expenseId}`, { signal })
  return response.data.data
}

export async function unlinkExpenseFromTask(expenseId, signal) {
  const response = await apiClient.patch(`/expenses/${expenseId}/task`, null, { signal })
  return response.data.data
}

export async function getMyExpenses(signal) {
  const response = await apiClient.get('/expenses/me', { signal })
  return response.data.data
}
