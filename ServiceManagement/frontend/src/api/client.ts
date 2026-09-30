import axios from 'axios';

const RAILWAY_API_URL =
  import.meta.env.VITE_RAILWAY_API_URL ||
  'https://keystone-servicemanagement-production.up.railway.app/api';

const RENDER_API_URL =
  import.meta.env.VITE_RENDER_API_URL ||
  'https://keystone-servicemanagement-tcam.onrender.com/api';

export const railwayApiClient = axios.create({
  baseURL: RAILWAY_API_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

export const renderApiClient = axios.create({
  baseURL: RENDER_API_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

const addAuthInterceptor = (client: typeof railwayApiClient) => {
  client.interceptors.request.use((config) => {
    const token = localStorage.getItem('keystone_token');

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  });

  client.interceptors.response.use(
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
};

addAuthInterceptor(railwayApiClient);
addAuthInterceptor(renderApiClient);

export async function dualRequest<T>(
  request: (client: typeof railwayApiClient) => Promise<T>
): Promise<T> {
  const [railwayResult, renderResult] = await Promise.all([
    request(railwayApiClient),
    request(renderApiClient)
  ]);

  return railwayResult;
}

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
