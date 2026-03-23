package org.cthub.backend.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.model.Image;
import org.cthub.backend.model.SeasonTeam;
import org.cthub.backend.model.SeasonTeamProfile;
import org.cthub.backend.model.TeamProfile;
import org.cthub.backend.repository.ImageRepository;
import org.cthub.backend.repository.SeasonTeamProfileRepository;
import org.cthub.backend.repository.SeasonTeamRepository;
import org.cthub.backend.repository.TeamProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeedProfilesTask implements CommandLineRunner {

    private final ImageRepository imageRepository;
    private final TeamProfileRepository profileRepository;
    private final SeasonTeamRepository teamRepository;
    private final SeasonTeamProfileRepository seasonTeamProfileRepository;


    private record SeasonTeamProfileRecord(
        String seasonId,
        String fllId,
        SeasonTeamProfile.AvatarMode avatarMode,
        String customAvatarImagePath
    ) {}

    private record TeamProfileRecord(
        String displayName,
        String customUrl,
        String imagePath,
        List<SeasonTeamProfileRecord> seasonTeams
    ) {}


    private static final List<TeamProfileRecord> TEAM_PROFILES = List.of(
        new TeamProfileRecord(
            "ROOTBOTS",
            "rootbots",
            "ROOTBOTS Avatar.png",
            List.of(
                new SeasonTeamProfileRecord("2025-26", "1011", SeasonTeamProfile.AvatarMode.INHERIT, null),
                new SeasonTeamProfileRecord("2024-25", "1046", SeasonTeamProfile.AvatarMode.INHERIT, null),
                new SeasonTeamProfileRecord("2023-24", "1042", SeasonTeamProfile.AvatarMode.INHERIT, null),
                new SeasonTeamProfileRecord("2022-23", "1096", SeasonTeamProfile.AvatarMode.CUSTOM, "WurzErLoesung.png")
            )
        )
    );


    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting SeedProfilesTask...");
        for (TeamProfileRecord teamProfile : TEAM_PROFILES) {
            seedTeamProfileWithImage(teamProfile);
        }
    }

    private UUID uploadImageGetUuid(String imagePath) throws Exception {
        ClassPathResource resource = new ClassPathResource(imagePath);
        byte[] bytes = resource.getInputStream().readAllBytes();

        Image testImage = Image.builder()
            .contentType("image/png")
            .originalFilename(imagePath)
            .sizeBytes((long) bytes.length)
            .data(bytes)
            .build();
        testImage = imageRepository.save(testImage);
        return testImage.getId();
    }

    private TeamProfile createTeamProfile(TeamProfileRecord teamProfile) throws Exception {
        TeamProfile profile = TeamProfile.builder()
            .displayName(teamProfile.displayName)
            .customUrl(teamProfile.customUrl)
            .profileImageUuid(uploadImageGetUuid(teamProfile.imagePath))
            .build();
        profile = profileRepository.save(profile);
        log.info("Created TeamProfile '{}' with customUrl '{}'", profile.getDisplayName(), profile.getCustomUrl());
        return profile;
    }

    private void seedTeamProfileWithImage(TeamProfileRecord teamProfile) throws Exception {

        TeamProfile profile = profileRepository.findByCustomUrl(teamProfile.customUrl).orElseGet(
            () -> {
                try {
                    return createTeamProfile(teamProfile);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );

        for(SeasonTeamProfileRecord stpr : teamProfile.seasonTeams) {
            Optional<SeasonTeam> seasonTeam = teamRepository.findBySeasonIdAndFllIdWithDetails(stpr.seasonId, stpr.fllId);
            if (seasonTeam.isPresent()) {
                if (seasonTeam.get().getSeasonTeamProfile() != null) {
                    log.info("SeasonTeam (Season: '{}', FLL ID: '{}') already has a linked TeamProfile. Skipping linking for TeamProfile '{}'",
                        stpr.seasonId, stpr.fllId, profile.getDisplayName());
                    continue;
                }
                SeasonTeamProfile teamProfileLink = SeasonTeamProfile.builder()
                    .seasonTeam(seasonTeam.get())
                    .teamProfile(profile)
                    .avatarMode(stpr.avatarMode)
                    .customAvatarId(stpr.customAvatarImagePath() != null ? uploadImageGetUuid(stpr.customAvatarImagePath()) : null)
                    .build();
                seasonTeam.get().setSeasonTeamProfile(teamProfileLink);
                seasonTeamProfileRepository.save(teamProfileLink);
                log.info("Linked TeamProfile '{}' to SeasonTeam (Season: '{}', FLL ID: '{}') with AvatarMode '{}'",
                    profile.getDisplayName(), stpr.seasonId, stpr.fllId, stpr.avatarMode);
            } else {
                log.warn("SeasonTeam not found for seasonId '{}' and fllId '{}'. Skipping linking for TeamProfile '{}'",
                    stpr.seasonId, stpr.fllId, profile.getDisplayName());
            }
        }
    }
}