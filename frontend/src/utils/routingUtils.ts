import type { CompetitionShortInfoDto, SeasonTeamDto } from '../api/generated';

/**
 * Generates the standard router link for a competition.
 */
export const getCompetitionLink = (comp: CompetitionShortInfoDto): string => {
    if (!comp?.season?.id || !comp?.urlPart) return '#';
    return `/competition/${comp.season.id}/${comp.urlPart}`;
};

/**
 * Generates the link for a team.
 * Prefers the custom profile URL if it exists, otherwise falls back to the standard route.
 * @param team The team data
 */
export const getTeamLink = (team: SeasonTeamDto): string => {
    return '#'; // Placeholder until we have team pages
    if (team.profile?.profileUrl) {
        return `/${team.profile?.profileUrl}`; // e.g., /let-s-robot
    }
    if (team.season?.id && team.fllId) {
        return `/team/${team.season?.id}/${team.fllId}`; // e.g., /team/2025-26/1007
    }
    return '#';
};