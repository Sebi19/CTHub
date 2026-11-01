import {getCountryFlagUrl} from "../../utils/competitionUtils.ts";

interface CountryFlagProps {
    country: string;
    height?: string | number;
}

export const CountryFlag = ({ country, height = '0.9em' }: CountryFlagProps) => {
    const flagUrl = getCountryFlagUrl(country);

    if (!flagUrl) {
        return null;
    }

    return <img src={flagUrl} alt="" style={{display: 'block', height, borderRadius: 1}} />;
}
