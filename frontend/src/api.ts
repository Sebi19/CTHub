// src/api.ts
import { Api } from "./api/generated"; // Adjust path to your generated code

// 1. Determine the Base URL ONE time
//    - If VITE_API_URL is set (Production/Railway), use it.
//    - Otherwise (Local), use window.location.origin to let the Vite Proxy handle it.
const backendUrl = import.meta.env.VITE_API_URL || window.location.origin;

// 2. Create the single instance
export const client = new Api({
    baseURL: backendUrl,
    // You can add global headers here later if needed (e.g. Auth)
});