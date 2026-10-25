// src/api.ts
import { Api } from "./api/generated"; // Adjust path to your generated code
import { reportProfileIncomplete } from "./features/auth/profileIncomplete";

// 1. Determine the Base URL ONE time
//    - If VITE_API_URL is set (Production/Railway), use it.
//    - Otherwise (Local), use window.location.origin to let the Vite Proxy handle it.
const backendUrl = import.meta.env.VITE_API_URL || window.location.origin;

// 2. Create the single instance
export const client = new Api({
    baseURL: backendUrl,
    // You can add global headers here later if needed (e.g. Auth)
});

// 3. Users who abandoned registration before naming themselves must finish their profile before
//    they may change anything; the backend enforces this and the UI follows it here.
client.instance.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error?.response?.data?.code === 'PROFILE_INCOMPLETE') {
            reportProfileIncomplete();
        }
        return Promise.reject(error);
    },
);