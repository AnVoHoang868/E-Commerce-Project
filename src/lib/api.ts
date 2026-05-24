const backendUrl = import.meta.env.VITE_BACKEND_URL || '';

type ApiOptions = Omit<RequestInit, 'body'> & {
    token?: string;
    body?: unknown;
};

export const apiBaseUrl = backendUrl;

export const apiRequest = async <T>(endpoint: string, options: ApiOptions = {}): Promise<T> => {
    if (!backendUrl) {
        throw new Error('Missing VITE_BACKEND_URL');
    }

    const { token, headers, body, ...requestOptions } = options;
    const requestBody: BodyInit | undefined = body === undefined
        ? undefined
        : typeof body === 'string'
            ? body
            : JSON.stringify(body);

    const response = await fetch(`${backendUrl}${endpoint}`, {
        ...requestOptions,
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            ...headers,
        },
        body: requestBody,
    });

    const data = await response.json().catch(() => null);

    if (!response.ok || data?.success === false) {
        const message = data?.message || `Request failed with status ${response.status}`;
        throw new Error(`${message} (${response.status})`);
    }

    return data as T;
};
