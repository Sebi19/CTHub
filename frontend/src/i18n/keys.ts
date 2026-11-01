import type { ParseKeys } from 'i18next';

/**
 * A translation key. Keep keys (not translated text) in component state so messages follow a
 * language switch - text translated once with `t()` stays in the old language.
 */
export type TranslationKey = ParseKeys;
