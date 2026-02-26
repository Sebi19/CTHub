import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import dayjs from 'dayjs';
import localizedFormat from 'dayjs/plugin/localizedFormat';
import 'dayjs/locale/de';
import 'dayjs/locale/en';

// Import translation files
import { de } from './locales/de';
import { en } from './locales/en';

dayjs.extend(localizedFormat);
export const resources = {
    de: {translation: de},
    en: {translation: en},
}

i18n
    // Detects user language from local storage, session storage, or navigator
    .use(LanguageDetector)
    // Passes i18n instance to react-i18next
    .use(initReactI18next)
    .init({
        resources,
        fallbackLng: 'de', // Default to German if detection fails
        interpolation: {
            escapeValue: false // React already escapes values to prevent XSS
        }
    });

i18n.on('languageChanged', (lng) => {
    dayjs.locale(lng);
});

dayjs.locale(i18n.resolvedLanguage || 'de');

i18n.services.formatter?.add('datetime', (value, lng, options) => {
    // If a specific format is passed (like 'LL'), use it. Otherwise, default to 'L' (short date)
    const format = options?.format || 'L';

    // Ensure we format the value using the correct language
    return dayjs(value).locale(lng as string).format(format);
});

export default i18n;