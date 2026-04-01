import type { Location, NavigateFunction } from 'react-router-dom';
import type {CompetitionShortInfoDto, SeasonTeamSummaryDto} from '../api/generated';

/**
 * Generates the standard router link for a competition.
 */
export const getCompetitionLink = (comp: CompetitionShortInfoDto): string => {
    return `/competition/${comp.season.id}/${comp.urlPart}`;
};

/**
 * Generates the link for a team.
 * Prefers the custom profile URL if it exists, otherwise falls back to the standard route.
 * @param team The team data
 */
export const getTeamLink = (team: SeasonTeamSummaryDto): string => {
    if (team.seasonTeamProfile?.profile) {
        return `/${team.seasonTeamProfile.profile.profileUrl}/${team.season.id}`;
    }
    return `/team/${team.season?.id}/${team.fllId}`; // e.g., /team/2025-26/1007
};

export const getCompetitionsListLink = (seasonId?: string | undefined): string => {
    if (!seasonId) return '/competitions';
    return `/competitions/${seasonId}`;
}

export const navigateBack = (location: Location<any>, navigate: NavigateFunction, fallback: string) => {
    const isDirectVisit = location.key === 'default';
    if (isDirectVisit) {
        navigate(fallback); // Escape hatch to your main app hub
    } else {
        navigate(-1); // Normal back behavior
    }
}