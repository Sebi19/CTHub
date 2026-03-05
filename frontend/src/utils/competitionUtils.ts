/**
 * Returns the standardized Mantine color for a competition type.
 */
export const getCompetitionTypeColor = (type?: string): string => {
    switch (type?.toLowerCase()) {
        case 'regional': return 'green';
        case 'qualification': return 'orange';
        case 'final': return 'red';
        default: return 'gray';
    }
};