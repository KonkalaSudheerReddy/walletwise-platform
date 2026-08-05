import type { AuthResponse, ProblemDetails, User } from './types';

const configuredBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '');
const API_BASE_URL = configuredBaseUrl ?? '';

type SessionStatus = 'restoring' | 'authenticated' | 'anonymous';

export interface SessionSnapshot {
  status: SessionStatus;
  user: User | null;
}

let accessToken: string | null = null;
let session: SessionSnapshot = { status: 'restoring', user: null };
let refreshPromise: Promise<boolean> | null = null;
const listeners = new Set<() => void>();

function publish(next: SessionSnapshot) {
  session = next;
  listeners.forEach((listener) => listener());
}

function acceptAuthentication(response: AuthResponse) {
  accessToken = response.accessToken;
  publish({ status: 'authenticated', user: response.user });
}

function clearAuthentication() {
  accessToken = null;
  publish({ status: 'anonymous', user: null });
}

export function subscribeSession(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getSessionSnapshot() {
  return session;
}

export class ApiError extends Error {
  constructor(public readonly problem: ProblemDetails) {
    super(problem.detail || problem.title);
    this.name = 'ApiError';
  }
}

async function problemFromResponse(response: Response): Promise<ProblemDetails> {
  const fallback: ProblemDetails = {
    title: response.statusText || 'Request failed',
    status: response.status,
    detail: 'The request could not be completed.'
  };

  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('json')) return fallback;

  try {
    const parsed = (await response.json()) as Partial<ProblemDetails>;
    return {
      ...fallback,
      ...parsed,
      status: parsed.status ?? response.status
    };
  } catch {
    return fallback;
  }
}

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  skipRefresh?: boolean;
}

function createInit(options: RequestOptions): RequestInit {
  const { skipRefresh, ...requestOptions } = options;
  void skipRefresh;
  const headers = new Headers(options.headers);
  headers.set('Accept', 'application/json');
  if (options.body !== undefined) headers.set('Content-Type', 'application/json');
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);

  return {
    ...requestOptions,
    headers,
    credentials: 'include',
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  };
}

async function parseSuccess<T>(response: Response): Promise<T> {
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

async function refreshAccessToken(): Promise<boolean> {
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
        headers: { Accept: 'application/json' }
      });
      if (!response.ok) {
        clearAuthentication();
        return false;
      }
      acceptAuthentication(await parseSuccess<AuthResponse>(response));
      return true;
    } catch {
      clearAuthentication();
      return false;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

export async function restoreSession() {
  if (session.status !== 'restoring') return;
  await refreshAccessToken();
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, createInit(options));

  if (response.status === 401 && !options.skipRefresh) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      const retried = await fetch(
        `${API_BASE_URL}${path}`,
        createInit({ ...options, skipRefresh: true })
      );
      if (retried.ok) return parseSuccess<T>(retried);
      throw new ApiError(await problemFromResponse(retried));
    }
  }

  if (!response.ok) throw new ApiError(await problemFromResponse(response));
  return parseSuccess<T>(response);
}

async function authenticate(path: string, body: unknown): Promise<User> {
  const response = await apiRequest<AuthResponse>(path, {
    method: 'POST',
    body,
    skipRefresh: true
  });
  acceptAuthentication(response);
  return response.user;
}

export function login(credentials: { email: string; password: string }) {
  return authenticate('/api/v1/auth/login', credentials);
}

export function registerAccount(payload: {
  displayName: string;
  email: string;
  password: string;
  preferredCurrency: string;
}) {
  return authenticate('/api/v1/auth/register', payload);
}

export async function logout() {
  try {
    await apiRequest<void>('/api/v1/auth/logout', { method: 'POST', skipRefresh: true });
  } finally {
    clearAuthentication();
    if (typeof BroadcastChannel !== 'undefined') {
      const channel = new BroadcastChannel('walletwise-session');
      channel.postMessage('logout');
      channel.close();
    }
  }
}

if (typeof BroadcastChannel !== 'undefined') {
  const channel = new BroadcastChannel('walletwise-session');
  channel.addEventListener('message', (event: MessageEvent<unknown>) => {
    if (event.data === 'logout') clearAuthentication();
  });
}
