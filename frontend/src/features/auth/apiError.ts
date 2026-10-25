import axios from 'axios';

export interface ApiErrorBody {
    code?: string;
    message?: string;
}

/** The `code` from the backend's {@link ApiExceptionHandler} payload, if this was an API rejection. */
export function apiErrorCode(error: unknown): string | undefined {
    if (!axios.isAxiosError(error)) return undefined;
    return (error.response?.data as ApiErrorBody | undefined)?.code;
}

export function apiErrorStatus(error: unknown): number | undefined {
    return axios.isAxiosError(error) ? error.response?.status : undefined;
}

export function apiErrorMessage(error: unknown): string {
    if (error instanceof Error && error.message) return error.message;
    return 'Unknown error';
}
