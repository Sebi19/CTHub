import { useEffect, useState } from 'react';
import {useParams, useNavigate, useLocation} from 'react-router-dom';
import {Container, Text, Loader, Center, Button} from '@mantine/core';
import {IconArrowLeft } from '@tabler/icons-react';
import type {SeasonTeamDetailsDto} from "../../api/generated.ts";
import {client} from "../../api.ts";
import {getCompetitionsListLink, getTeamLink, navigateBack} from "../../utils/routingUtils.ts";
import {useDocumentTitle} from "@mantine/hooks";
import {useTranslation} from "react-i18next";
import {SeasonTeamDetails} from "./SeasonTeamDetails.tsx";

export const SeasonTeamDetailPage = () => {
    const { seasonId, fllId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const {t} = useTranslation();

    const [teamDetails, setTeamDetails] = useState<SeasonTeamDetailsDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useDocumentTitle(t('app.season_team.detail.doc_title', {teamName: teamDetails?.name || '', seasonId: teamDetails?.season?.id || ''}))

    useEffect(() => {
        if (!seasonId || !fllId) return;

        // Hook this up to your generated API client method!
        client.api.getTeamDetails(seasonId, fllId)
            .then((res) => {
                setTeamDetails(res.data);
                setIsLoading(false);
            })
            .catch(() => {
                setIsLoading(false);
            });
    }, [seasonId, fllId]);

    if (teamDetails?.seasonTeamProfile?.profile.profileUrl) {
        navigate(getTeamLink(teamDetails), {replace: true});
        return null; // Don't render anything while redirecting
    }


    if (isLoading) return <Center h="50vh"><Loader /></Center>;
    if (!teamDetails) return <Center h="50vh"><Text c="red">Team not found</Text></Center>;

    const handleBackNavigation = () => {
        navigateBack(location, navigate, getCompetitionsListLink(seasonId))
    };

    return (
        <Container size="xl" py="xl">
            {/* Top Navigation */}
            <Button
                variant="subtle"
                color="gray"
                leftSection={<IconArrowLeft size={16} />}
                onClick={handleBackNavigation}
                mb="md"
                px={0}
            >
                {t('app.season_team.detail.back')}
            </Button>

            <SeasonTeamDetails teamDetails={teamDetails} />
        </Container>
    );
};