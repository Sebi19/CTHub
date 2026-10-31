import type { TFunction } from 'i18next';
import { apiErrorCode, apiErrorStatus } from '../auth/apiError.ts';

const KEYS = {
    PASSWORD_TOO_SHORT: 'password_too_short',
    PASSWORD_TOO_LONG: 'password_too_long',
    PASSWORD_PWNED: 'password_pwned',
    PASSWORD_REQUIRED: 'password_required',
    TOTP_ALREADY_ENABLED: 'totp_already_enabled',
    TOTP_INVALID: 'totp_invalid',
    TOTP_NOT_PENDING: 'totp_not_pending',
    TOTP_UNAVAILABLE: 'totp_unavailable',
    EMAIL_UNCHANGED: 'email_unchanged',
    EMAIL_TAKEN: 'email_taken',
    LINK_INVALID: 'link_invalid',
    PASSKEY_LIMIT: 'passkey_limit',
    PASSKEY_EXISTS: 'passkey_exists',
    PASSKEY_INVALID: 'passkey_invalid',
    PASSKEY_CEREMONY_EXPIRED: 'passkey_ceremony_expired',
    STEP_UP_REQUIRED: 'step_up_required',
} as const;

/** Turns a rejected account API call into a message for the user. */
export function accountErrorMessage(t: TFunction, error: unknown): string {
    if (apiErrorStatus(error) === 429) return t('app.account.errors.rate_limited');
    const code = apiErrorCode(error);
    if (code && code in KEYS) {
        return t(`app.account.errors.${KEYS[code as keyof typeof KEYS]}` as const);
    }
    return t('app.account.errors.generic');
}
