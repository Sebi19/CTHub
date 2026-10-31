type Handler = () => Promise<boolean>;

let handler: Handler | null = null;
let pending: Promise<boolean> | null = null;

/**
 * Sensitive account changes answer `403 STEP_UP_REQUIRED` until the session re-proved its identity.
 * The axios interceptor in `api.ts` lives outside React, so it asks through this hook and the
 * mounted `StepUpModal` does the actual verification.
 */
export function onStepUpRequired(fn: Handler | null) {
    handler = fn;
}

/** Resolves `true` once verified, `false` if the user gave up. Parallel callers share one prompt. */
export function requestStepUp(): Promise<boolean> {
    if (!handler) return Promise.resolve(false);
    pending ??= handler().finally(() => {
        pending = null;
    });
    return pending;
}
