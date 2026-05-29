import { apiBaseUrl } from './api';

export type ChatRole = 'user' | 'assistant';

export type ChatStreamMessage = {
    role: ChatRole;
    content: string;
};

type StreamChatOptions = {
    message: string;
    history: ChatStreamMessage[];
    token?: string;
    signal?: AbortSignal;
    onToken: (token: string) => void;
};

const readJsonText = (value: unknown): string => {
    if (!value || typeof value !== 'object') return '';

    const data = value as Record<string, unknown>;
    const candidates = [
        data.content,
        data.message,
        data.answer,
        data.response,
        data.text,
        data.data,
        (data.delta as Record<string, unknown> | undefined)?.content,
    ];

    const text = candidates.find((candidate) => typeof candidate === 'string');
    return typeof text === 'string' ? text : '';
};

const readStreamPayload = (payload: string) => {
    const trimmed = payload.trim();
    if (!trimmed || trimmed === '[DONE]') return '';

    try {
        const parsed = JSON.parse(trimmed);
        return readJsonText(parsed) || trimmed;
    } catch {
        return payload;
    }
};

export const streamChatResponse = async ({
    message,
    history,
    token,
    signal,
    onToken,
}: StreamChatOptions) => {
    if (!apiBaseUrl) {
        throw new Error('Missing VITE_BACKEND_URL');
    }

    const params = new URLSearchParams({ message });
    const response = await fetch(`${apiBaseUrl}/chat?${params.toString()}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream, text/plain, application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ message, history }),
        signal,
    });

    if (!response.ok) {
        const errorText = await response.text().catch(() => '');
        throw new Error(errorText || `Request failed with status ${response.status}`);
    }

    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
        const data = await response.json();
        const text = readJsonText(data);
        if (text) onToken(text);
        return;
    }

    if (!response.body) {
        const text = await response.text();
        if (text) onToken(text);
        return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        if (contentType.includes('text/event-stream')) {
            const events = buffer.split('\n\n');
            buffer = events.pop() || '';

            events.forEach((event) => {
                event.split('\n').forEach((line) => {
                    if (!line.startsWith('data:')) return;
                    const tokenText = readStreamPayload(line.replace(/^data:\s?/, ''));
                    if (tokenText) onToken(tokenText);
                });
            });
        } else {
            onToken(buffer);
            buffer = '';
        }
    }

    const remaining = readStreamPayload(buffer);
    if (remaining) onToken(remaining);
};
