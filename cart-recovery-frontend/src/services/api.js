const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
).replace(/\/$/, '')

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  })

  if (!response.ok) {
    const errorBody = await response.text()
    throw new Error(errorBody || `Request failed with status ${response.status}`)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}

export const productApi = {
  getAll: () => request('/api/products'),
  getAvailable: () => request('/api/products/available'),
  getById: (productId) => request(`/api/products/${productId}`),
  getByName: (name) =>
    request(`/api/products/by-name?name=${encodeURIComponent(name)}`),
  search: (keyword) =>
    request(`/api/products/search?keyword=${encodeURIComponent(keyword)}`),
  create: (product) =>
    request('/api/products', {
      method: 'POST',
      body: JSON.stringify(product),
    }),
  update: (productId, product) =>
    request(`/api/products/${productId}`, {
      method: 'PUT',
      body: JSON.stringify(product),
    }),
  updateStock: (productId, quantity) =>
    request(`/api/products/${productId}/stock?quantity=${quantity}`, {
      method: 'PATCH',
    }),
  remove: (productId) =>
    request(`/api/products/${productId}`, { method: 'DELETE' }),
}

export const authApi={

  login : async (credentials) => 

    request('/api/auth/login', 
      {
        method:"POST",
        body:JSON.stringify(credentials),
      }
    ),
   
  signup : async (data)=>
   request('/api/customers',
  {
    method:"POST",
    body:JSON.stringify(data)
  }
)
}

export const cartApi = {
  getAll: () => request('/api/carts'),
  getById: (cartId) => request(`/api/carts/${cartId}`),
  getByCustomer: (customerId) =>
    request(`/api/carts/customer/${customerId}`),
  create: (cart) =>
    request('/api/carts', {
      method: 'POST',
      body: JSON.stringify(cart),
    }),

   
  update: (cartId, cart) =>
    request(`/api/carts/${cartId}`, {
      method: 'PUT',
      body: JSON.stringify(cart),
    }),
  checkout: (cartId) =>
    request(`/api/carts/${cartId}/checkout`, { method: 'POST' }),
  remove: (cartId) => request(`/api/carts/${cartId}`, { method: 'DELETE' }),
  getItems: (cartId) => request(`/api/carts/${cartId}/items`),
  addItem: (cartId, item) =>
    request(`/api/carts/${cartId}/items`, {
      method: 'POST',
      body: JSON.stringify(item),
    }),
  updateItem: (cartId, cartItemId, item) =>
    request(`/api/carts/${cartId}/items/${cartItemId}`, {
      method: 'PUT',
      body: JSON.stringify(item),
    }),
  removeItem: (cartId, cartItemId) =>
    request(`/api/carts/${cartId}/items/${cartItemId}`, {
      method: 'DELETE',
    }),
  clear: (cartId) =>
    request(`/api/carts/${cartId}/items`, { method: 'DELETE' }),

validateDiscount: (cartId, code) =>
    request(
      `/api/carts/${cartId}/validate-discount?code=${encodeURIComponent(code)}`
    ),
}

export { API_BASE_URL }
