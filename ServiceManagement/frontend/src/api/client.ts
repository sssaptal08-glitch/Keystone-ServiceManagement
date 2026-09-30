import axios, { type AxiosRequestConfig } from 'axios';

const RAILWAY_API_URL =
  import.meta.env.VITE_RAILWAY_API_URL ||
  'https://keystone-servicemanagement-production.up.railway.app/api';

const RENDER_API_URL =
  import.meta.env.VITE_RENDER_API_URL ||
  'https://keystone-servicemanagement-tcam.onrender.com/api';

export const apiClient = axios.create({
  baseURL: RAILWAY_API_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Send the same request to Render as well.
apiClient.interceptors.request.use(async (config) => {
  const token = localStorage.getItem('keystone_token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  const renderConfig: AxiosRequestConfig = {
    ...config,
    baseURL: RENDER_API_URL,
    url: config.url,
    method: config.method,
    params: config.params,
    data: config.data,
    headers: {
      ...config.headers
    }
  };

  // Send to Render without affecting the Railway request.
  axios.request(renderConfig).catch((error) => {
    console.error('Render backend request failed:', error);
  });

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('keystone_token');
      localStorage.removeItem('keystone_user');

      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;

    if (data?.details?.length) {
      return data.details.join(', ');
    }

    if (data?.message) {
      return data.message;
    }
  }

  return 'Something went wrong. Please try again.';
}
