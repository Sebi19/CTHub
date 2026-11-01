import { apiErrorCode, apiErrorStatus } from '../auth/apiError.ts';
import type { TranslationKey } from '../../i18n/keys.ts';

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

/** Translation key of the message for a rejected account API call. */
export function accountErrorKey(error: unknown): TranslationKey {
    if (apiErrorStatus(error) === 429) return 'app.account.errors.rate_limited';
    const code = apiErrorCode(error);
    if (code && code in KEYS) {
        return `app.account.errors.${KEYS[code as keyof typeof KEYS]}`;
    }
    return 'app.account.errors.generic';
}
