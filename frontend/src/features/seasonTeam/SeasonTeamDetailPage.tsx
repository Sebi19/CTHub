import { useEffect, useState } from 'react';
import {useParams, useNavigate, useLocation} from 'react-router-dom';
import {Container, Loader, Center} from '@mantine/core';
import type {SeasonTeamDetailsDto} from "../../api/generated.ts";
import {client} from "../../api.ts";
import {getCompetitionsListLink, getTeamLink, navigateBack} from "../../utils/routingUtils.ts";
import {useDocumentTitle} from "@mantine/hooks";
import {useTranslation} from "react-i18next";
import {SeasonTeamDetails} from "./SeasonTeamDetails.tsx";
import {NotFoundPage} from "../error/NotFoundPage.tsx";
import {ServerErrorPage} from "../error/ServerErrorPage.tsx";
import {NavigateBackButton} from "../common/navigation/NavigateBackButton.tsx";

export const SeasonTeamDetailPage = () => {
    const { seasonId, fllId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const {t} = useTranslation();

    const [teamDetails, setTeamDetails] = useState<SeasonTeamDetailsDto | null>(null);
    const [errorCode, setErrorCode] = useState<number | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        setTeamDetails(null);
        setErrorCode(null);
        setIsLoading(true);
        if (!seasonId || !fllId) return;

        // Hook this up to your generated API client method!
        client.api.getTeamDetails(seasonId, fllId)
            .then((res) => {
                setTeamDetails(res.data);
                setIsLoading(false);
            })
            .catch((error) => {
                const status = error.response?.status || 500;

                setErrorCode(status);
                setIsLoading(false);
            });
    }, [seasonId, fllId]);

    if (teamDetails?.seasonTeamProfile?.profile.profileUrl) {
        navigate(getTeamLink(teamDetails), {replace: true});
        return null; // Don't render anything while redirecting
    }

    useDocumentTitle(t('app.season_team.detail.doc_title', {teamName: teamDetails?.name || '', seasonId: teamDetails?.season?.id || ''}))

    const handleBackNavigation = () => {
        navigateBack(location, navigate, getCompetitionsListLink(seasonId))
    };


    if (isLoading) return <Center h="50vh"><Loader /></Center>;

    if (errorCode === 404) {
        return <NotFoundPage handleBackNavigation={handleBackNavigation} />;
    }

    if (errorCode  || !teamDetails) {
        return <ServerErrorPage handleBackNavigation={handleBackNavigation} />;
    }

    return (
        <Container size="xl" py="xl">
            {/* Top Navigation */}
            <NavigateBackButton handleBackNavigation={handleBackNavigation} />

            <SeasonTeamDetails teamDetails={teamDetails} />
        </Container>
    );
};