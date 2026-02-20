import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// Import translation files
import { de } from './locales/de';
import { en } from './locales/en';

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

export default i18n;