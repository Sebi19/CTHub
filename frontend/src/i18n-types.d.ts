import 'i18next';
import { de } from './i18n/locales/de';

declare module 'i18next' {
    interface CustomTypeOptions {
        defaultNS: 'translation';
        resources: {
            translation: typeof de;
        }
    }
}