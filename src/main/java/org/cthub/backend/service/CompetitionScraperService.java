package org.cthub.backend.service;

import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Link;
import org.cthub.backend.model.Nomination;
import org.cthub.backend.model.Place;
import org.cthub.backend.model.Season;
import org.cthub.backend.model.SeasonTeam;
import org.cthub.backend.model.RobotGameResult;
import org.cthub.backend.repository.CompetitionRepository;
import org.cthub.backend.repository.NominationRepository;
import org.cthub.backend.repository.PlaceRepository;
import org.cthub.backend.repository.RobotGameResultRepository;
import org.cthub.backend.repository.SeasonRepository;
import org.cthub.backend.repository.SeasonTeamRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class CompetitionScraperService {

    private static final Logger LOG = LoggerFactory.getLogger(CompetitionScraperService.class);

    // Constants from your Python script
    private static final String BASE_URL = "https://www.first-lego-league.org";
    private static final String LOCATIONS_URL = "https://www.first-lego-league.org/de/austragungsorte";

    private static final String SEASON_PART_TEMPLATE = "de/challenge-%s";
    private static final String RESULTS_BASE_URL = "https://evaluation.hands-on-technology.org/de/score/";
    private static final String RG_RESULTS_BASE_URL = "https://evaluation.hands-on-technology.org/de/rg-score/";


    private final SeasonRepository seasonRepository;
    private final CompetitionRepository competitionRepository;
    private final SeasonTeamRepository seasonTeamRepository;
    private final RobotGameResultRepository robotGameResultRepository;
    private final PlaceRepository placeRepository;
    private final NominationRepository nominationRepository;

    public CompetitionScraperService(SeasonRepository seasonRepository, CompetitionRepository competitionRepository, SeasonTeamRepository seasonTeamRepository, RobotGameResultRepository robotGameResultRepository, PlaceRepository placeRepository, NominationRepository nominationRepository) {
        this.seasonRepository = seasonRepository;
        this.competitionRepository = competitionRepository;
        this.seasonTeamRepository = seasonTeamRepository;
        this.robotGameResultRepository = robotGameResultRepository;
        this.placeRepository = placeRepository;
        this.nominationRepository = nominationRepository;
    }

    @Transactional
    @Async
    public void scrapeCurrentSeason() {
        // 1. Ensure Season exists or create new
        Season season = seasonRepository.findByActiveTrue()
                .orElse(Season.builder()
                    .id("2025-2026")
                    .name("Unearthed")
                    .startYear(2025)
                    .active(true)
                    .build());
        seasonRepository.save(season);

        LOG.info("Starting scrape for Season: {}", season.getId());

        try {
            scrapeCompetitionsList(season);
        } catch (IOException e) {
            LOG.error("Failed to scrape locations page", e);
        }
    }

    private String getSeasonPart(Season season) {
        // Get season part like "de/challenge-2024-25"
        int startYear = season.getStartYear();
        int endYearShort = (startYear + 1) % 100;
        String seasonPart = String.format("%d-%d", startYear, endYearShort);
        return String.format(SEASON_PART_TEMPLATE, seasonPart);
    }

    private void scrapeCompetitionsList(Season season) throws IOException {
        Document doc = Jsoup.connect(LOCATIONS_URL).get();

        // Python lines 147-149: Parsing country tables
        parseRegionalTable(doc, "challenge-table-de", "DE", season);
        parseRegionalTable(doc, "challenge-table-at", "AT", season);
        parseRegionalTable(doc, "challenge-table-ch", "CH", season);

        // Python line 151: "Qualifikationswettbewerbe"
        // This is tricky in Jsoup: Find h4 with text, then get next sibling div
        Element qualiHeader = doc.select("h4:contains(Qualifikationswettbewerbe)").first();
        if (qualiHeader != null) {
            Element qualiDiv = qualiHeader.nextElementSibling();
            if (qualiDiv != null) {
                parseRows(qualiDiv, null, season, Competition.CompetitionType.QUALIFICATION);
            }
        }

        Element finalHeader = doc.select("h4:contains(Finale)").first();
        if (finalHeader != null) {
            Element finalDiv = finalHeader.nextElementSibling();
            if (finalDiv != null) {
                parseRows(finalDiv, null, season, Competition.CompetitionType.FINAL);
            }
        }
    }

    private void parseRegionalTable(Document doc, String divId, String country, Season season) {
        Element div = doc.getElementById(divId);
        if (div != null) {
            parseRows(div, country, season, Competition.CompetitionType.REGIONAL);
        }
    }

    private void parseRows(Element container, String country, Season season, Competition.CompetitionType competitionType) {
        // Python line 154: .findChildren("a", {"class":"row"})
        Elements rows = container.select("a.row");

        String seasonPart = getSeasonPart(season);

        for (Element row : rows) {
            String href = row.attr("href");

            // Python line 161: if(href.startswith(YEAR_PART))
            if (href.contains(seasonPart)) {
                // name is in first child div only
                String name = Optional.ofNullable(row.select("div").first())
                        .map(Element::text)
                        .orElseThrow(() -> new IllegalStateException("No name div found"));
                String urlPart = href.substring(href.lastIndexOf("/") + 1); // Extract "regio-xyz"

                LOG.info("Found Event: {} [{}]", name, country);

                // Deep Scrape: Go to the event page to find the results link
                processEventPage(season, name, country, urlPart, competitionType);
            }
        }
    }

    private void processEventPage(Season season, String name, String country, String urlPart, Competition.CompetitionType competitionType) {
        String eventUrl = BASE_URL + "/" + getSeasonPart(season) + "/" + urlPart;
        LOG.info("Processing Event: {}", name);

        try {
            Document eventDoc = Jsoup.connect(eventUrl).get();

            // 1. ALWAYS Initialize the Competition (Upsert Logic)
            Competition comp = competitionRepository.findByUrlPart(urlPart)
                .orElse(Competition.builder()
                    .urlPart(urlPart)
                    .season(season)
                    .build());

            // 2. Update Basic Metadata
            comp.setName(name);
            comp.setCountry(country); // Might be null for Qualifications, that's okay
            comp.setType(competitionType);

            // 3. Scrape Detailed Metadata (Date, Location, Contact)
            // We use helper methods to find these based on labels in the HTML
            comp.setDate(parseDate(eventDoc)); // You'll need to add a parser for String -> LocalDate
            comp.setLocation(findMetadata(eventDoc, "Veranstaltungsort"));

            // Contact is complex, often separate fields. Let's try to find a name/email.
            Competition.ContactInfo contact = new Competition.ContactInfo();
            contact.setContactName(findMetadata(eventDoc, "Kontakt"));
            contact.setContactEmail(findEmail(eventDoc));
            comp.setContact(contact);

            List<Link> webLinks = findLinks(eventDoc, "Weblinks");
            comp.setLinks(webLinks);

            List<Link> resultLinks = findLinks(eventDoc, "Ergebnisse");
            boolean actuallyHasResults = false;

            if (!resultLinks.isEmpty()) {
                Link mainResultLink = resultLinks.getFirst();
                String href = mainResultLink.getUrl();
                String resultsUrlPart = href.substring(href.lastIndexOf("/") + 1);

                comp.setResultsUrlPart(resultsUrlPart);

                // 5. Verify Content: Don't just trust the link exists. Check the page!
                actuallyHasResults = checkResultsPageContent(RG_RESULTS_BASE_URL + resultsUrlPart);
            }
            comp.setResultsAvailable(actuallyHasResults);

            // 6. Save EVERYTHING (Even if no results found)
            competitionRepository.save(comp);

            scrapeTeams(eventDoc, comp);

            if (actuallyHasResults) {
                scrapeRobotgameResults(comp);
                scrapeOverview(comp);
            }

            LOG.info("   -> Saved Metadata for '{}' (Results: {})", name, actuallyHasResults);

        } catch (Exception e) {
            LOG.error("Failed to process event page: " + eventUrl, e);
        }
    }

    /**
     * Follows the results link and checks if a Scoreboard table actually exists.
     * Corresponds to Python: if(len(tables_list) == 0)
     */
    private boolean checkResultsPageContent(String fullResultsUrl) {
        try {
            Document resultsDoc = Jsoup.connect(fullResultsUrl).get();
            // Look for the specific scoreboard table class used by FLL evaluation
            Elements tables = resultsDoc.select("table.scoreboard");
            return !tables.isEmpty();
        } catch (IOException e) {
            LOG.warn("   -> Could not reach results page: {}", fullResultsUrl);
            return false;
        }
    }

    /**
     * Helper to find text following a label like "Datum:" or "Ort:"
     * It looks for a <strong> or <label> containing the keyword, then gets the text after it.
     */
    private String findMetadata(Document doc, String keyword) {
        Elements elements = doc.select("strong:contains(" + keyword + ")");

        for (Element el : elements) {
            String text = "";

            // Strategy 1: Look at the Next Sibling
            if (el.nextSibling() != null) {
                text = el.nextSibling().toString();
            }

            // Strategy 2: Look at Parent (The most common case for "Kontakt")
            // Example HTML: <div><strong>Kontakt:</strong> John Doe <a href="...">john@doe.com</a></div>
            if (text.trim().isEmpty() && el.parent() != null) {
                // 1. Clone the parent so we don't destroy the original document
                Element clone = el.parent().clone();

                // 2. Remove the Label itself (e.g. "Kontakt:")
                clone.select("strong:contains(" + keyword + ")").remove();

                // 3. Remove any Links (Emails/Websites) so they don't pollute the name
                clone.select("a").remove();

                // 4. Get the text that remains
                // wholeText() preserves newlines!
                text = clone.wholeText();
            }

            if (!text.trim().isEmpty() && !text.trim().equals(":")) {
                return cleanText(text);
            }
        }
        return null;
    }

    private String findEmail(Document doc) {
        Elements elements = doc.select("strong:contains(Kontakt)");
        for (Element el : elements) {
            Element parent = el.parent();
            if (parent != null) {
                Element mailLink = doc.select("a[href^=mailto:]").first();
                if (mailLink != null) {
                    return mailLink.attr("href").replace("mailto:", "");
                }
            }
        }
        return null;
    }

    private List<Link> findLinks(Document doc, String keyword) {
        List<Link> foundLinks = new ArrayList<>();

        // Find the header/label (e.g. <strong>Weblinks</strong>)
        Elements elements = doc.select("strong:contains(" + keyword + ")");

        for (Element el : elements) {
            // Scope: Where do we look for <a> tags?
            // Usually FLL puts them in a List <ul> right after the header,
            // OR they are simple sibling <a> tags.

            Element container = null;

            // Case A: The header is inside the same block as the links
            // <div><h5>Weblinks</h5> <a...> <a...></div>
            if (el.parent() != null) {
                container = el.parent();
            }

            if (container != null) {
                // Select all links in that scope
                Elements aTags = container.select("a");
                for (Element a : aTags) {
                    String href = a.attr("href");
                    String text = a.text().trim();

                    // Filter out garbage (mailto links, empty links)
                    if (!href.startsWith("mailto:") && !href.isEmpty() && !href.equals("http://") && !href.equals("https://")) {
                        foundLinks.add(Link.builder()
                            .url(href)
                            .label(text.isEmpty() ? "Link" : text) // Fallback label
                            .build());
                    }
                }
            }
        }
        return foundLinks;
    }

    private java.time.LocalDate parseDate(Document doc) {
        String dateString = findMetadata(doc, "Termin");
        if (dateString != null) {
            try {
                return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            } catch (Exception e) {
                LOG.warn("Could not parse date: {}", dateString);
            }
        }
        return null;
    }

    private String cleanText(String input) {
        if (input == null) return null;
        return input.replaceAll("[:\\t]+", " ") // Remove weird tabs/colons
            .trim(); // Only trim the start/end, keep the middle \n
    }

    private void scrapeTeams(Document doc, Competition comp) {
        // Find the specific container for teams
        // Using the class from your screenshot
        Elements rows = doc.select("div.ce_table.teams div.row:not(.header)");

        if (rows.isEmpty()) return;

        LOG.info("   -> Found {} registered teams", rows.size());

        for (Element row : rows) {
            try {
                // Columns: 1=ID, 2=Name, 3=Institution, 4=City/Links

                String fllId = row.select(">div:nth-of-type(1)").text().trim();
                String teamName = row.select(">div:nth-of-type(2)").text().trim();
                String institution = row.select(">div:nth-of-type(3)").text().trim();

                // The complex column
                Element buttonCol = row.select(">div:nth-of-type(4)").first();
                String city = "";
                List<Link> teamLinks = new ArrayList<>();

                if (buttonCol != null) {
                    // 1. Extract City
                    city = buttonCol.select("span.text").text().trim();

                    // 2. Extract Hidden Links (Popup)
                    Element infoButton = buttonCol.select("a.info.inline").first();
                    if (infoButton != null) {
                        // The HTML is hidden inside this attribute!
                        String hiddenHtml = infoButton.attr("data-inline-content");
                        if (!hiddenHtml.isEmpty()) {
                            // Parse the attribute content as if it were a mini-website
                            Document popupDoc = Jsoup.parse(hiddenHtml);
                            teamLinks = findLinks(popupDoc, "Social Media"); // Reuse your existing findLinks!

                            // If "Social Media" keyword isn't there, just grab all links
                            if (teamLinks.isEmpty()) {
                                for (Element a : popupDoc.select("a")) {
                                    String href = a.attr("href").trim();
                                    String label = a.text().trim();

                                    // FILTER: Ignore empty links, mailto, or placeholder "http://"
                                    if (!href.isEmpty()
                                        && !href.equals("http://")
                                        && !href.equals("https://")
                                        && !href.startsWith("mailto:")) {

                                        teamLinks.add(Link.builder()
                                            .url(href)
                                            // If label is empty, use the domain name instead of generic "Link"
                                            .label(label.isEmpty() ? href : label)
                                            .build());
                                    }
                                }
                            }
                        }
                    }
                }

                // UPSERT SeasonTeam
                // We use the SeasonTeamRepository to find by Season + FLL ID
                SeasonTeam team = seasonTeamRepository.findBySeasonIdAndFllId(comp.getSeason().getId(), fllId)
                    .orElse(SeasonTeam.builder()
                        .season(comp.getSeason())
                        .fllId(fllId)
                        .build());

                // Update details
                team.setName(teamName);
                team.setInstitution(institution);
                team.setCity(city);
                team.setLinks(teamLinks);

                if (comp.getType().equals(Competition.CompetitionType.REGIONAL)) {
                    team.setCountry(comp.getCountry());
                }

                // Create the Link: Team <-> Competition
                // We initialize the set if it's null (Hibernate might return null sometimes)
                if (team.getRegisteredCompetitions() == null) {
                    team.setRegisteredCompetitions(new HashSet<>());
                }

                team.getRegisteredCompetitions().add(comp);

                seasonTeamRepository.save(team);

            } catch (Exception e) {
                LOG.warn("Failed to parse team row: {}", e.getMessage());
            }
        }
    }

    private void scrapeRobotgameResults(Competition comp) {
        String resultsUrl = RG_RESULTS_BASE_URL + comp.getResultsUrlPart();
        try {
            Document doc = Jsoup.connect(resultsUrl).get();
            Element table = doc.select("table.scoreboard").first();

            if (table == null) return;

            LOG.info("   -> Scraping scores for '{}'", comp.getName());

            // 1. Get Headers (Python: header_cols = header_row.findChildren("th"))
            Elements headerCols = table.select("thead tr th");
            List<String> headers = headerCols.stream()
                .map(e -> cleanText(e.text()))
                .toList();

            // 2. Iterate Rows
            Elements rows = table.select("tbody tr");
            for (Element row : rows) {
                // Python: team_id = ... findChildren("span", {"class":"team-id"}) ... [1:-1]
                Element teamIdSpan = row.select("th span.team-id").first();
                if (teamIdSpan == null) continue;

                // Strip brackets "(1234)" -> "1234"
                String rawId = cleanText(teamIdSpan.text()).replaceAll("[\\[\\]()]", "");

                // Find the team (Must exist from previous step)
                Optional<SeasonTeam> teamOpt = seasonTeamRepository.findBySeasonIdAndFllId(comp.getSeason().getId(), rawId);

                if (teamOpt.isPresent()) {
                    SeasonTeam team = teamOpt.get();

                    // Upsert Logic
                    RobotGameResult result = robotGameResultRepository.findByCompetitionAndSeasonTeam(comp, team)
                        .orElse(RobotGameResult.builder()
                            .competition(comp)
                            .seasonTeam(team)
                            .build());

                    // Python: score_cols = row.findChildren("td")
                    Elements scoreCols = row.select("td");

                    for (int i = 0; i < scoreCols.size(); i++) {
                        // Python Logic: match headers[i + 1]
                        if (i + 1 >= headers.size()) break; // Safety check

                        String header = headers.get(i + 1);
                        String valueStr = cleanText(scoreCols.get(i).text());
                        int parsedValue = parseScore(valueStr);

                        Integer value = parsedValue == 0 ? null : parsedValue;

                        switch (header) {
                            case "VR I": result.setPr1(Optional.ofNullable(value).orElse(0)); break;
                            case "VR II": result.setPr2(Optional.ofNullable(value).orElse(0)); break;
                            case "VR III": result.setPr3(Optional.ofNullable(value).orElse(0)); break;
                            case "Beste VR": result.setBestPr(Optional.ofNullable(value).orElse(0)); break;
                            case "AF": result.setR16(value); break;
                            case "VF": result.setQf(value); break;
                            case "HF": result.setSf(value); break;
                            case "F I": result.setF1(value); break;
                            case "F II": result.setF2(value); break;
                            // Rank is usually separate, Python script didn't explicitly map it in the loop
                        }
                    }
                    result.setRank(null);
                    robotGameResultRepository.save(result);
                }
            }

        } catch (IOException e) {
            LOG.error("Failed to scrape results: {}", resultsUrl, e);
        }
    }

    private int parseScore(String text) {
        try {
            if (text == null || text.isEmpty() || text.equals("-")) return 0;
            return Integer.parseInt(text.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void scrapeOverview(Competition comp) {
        String resultsUrl = RESULTS_BASE_URL + comp.getResultsUrlPart();
        try {
            Document doc = Jsoup.connect(resultsUrl).get();
            LOG.info("   -> Scraping awards overview for '{}'", comp.getName());

            Element table = doc.select("div.final-award-table").first();
            if (table == null) return;

            LOG.info("   -> Scraping awards overview for '{}' (Clearing old data first)", comp.getName());
            placeRepository.deleteByCompetition(comp);
            nominationRepository.deleteByCompetition(comp);

            // --- 1. CHAMPION ROW ---
            Element championRow = table.select("div.row.champion").first();

            if (championRow != null) {

                Element qualifierCountElement = championRow.select("div.next-round-message").first();
                int qualifierCount = 1; // Default to 1 since regex doesn't match for 1 qualifier
                if (qualifierCountElement != null) {
                    String text = qualifierCountElement.text();
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Platz \\d+ bis (\\d+)");
                    java.util.regex.Matcher matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        String countStr = matcher.group(1);
                        try {
                            qualifierCount = Integer.parseInt(countStr);
                        } catch (NumberFormatException e) {
                            // Ignore parse errors
                        }
                    }
                }

                // A. Winner (Place 1)
                Element winnerDiv = championRow.select("div.winner").first();
                if (winnerDiv != null) {
                    savePlace(comp, winnerDiv.text().trim(), 1, qualifierCount >= 1);
                }

                // B. Runners Up (Place 2 & 3) in col-md-5
                Element detailsDiv = championRow.select("div.col-md-5").first();
                if (detailsDiv != null) {
                    // Split by <br> to handle lines properly
                    String[] lines = detailsDiv.html().split("<br\\s*/?>");
                    for (String line : lines) {
                        // Jsoup parse to remove any remaining tags from the line
                        String text = Jsoup.parse(line).text().trim();
                        if (text.startsWith("2. Platz:")) {
                            savePlace(comp, text.replace("2. Platz:", "").trim(), 2, qualifierCount >= 2);
                        } else if (text.startsWith("3. Platz:")) {
                            savePlace(comp, text.replace("3. Platz:", "").trim(), 3, qualifierCount >= 3);
                        } else if (text.startsWith("Weiterhin qualifiziert: ")) {
                            String additionalTeams = text.replace("Weiterhin qualifiziert: ", "").trim();
                            String[] teamNames = additionalTeams.split(",");
                            int place = 4; // Start from 4th place for additional qualifiers
                            for (String teamName : teamNames) {
                                savePlace(comp, teamName.trim(), place, true); // 0 indicates advancing but no specific place
                                place++;
                            }
                        }
                    }
                }

                // --- 2. CATEGORY ROWS ---
                // Iterate all rows that are NOT the header or the champion row
                Elements categoryRows = table.select("div.row:not(.header)");

                for (Element row : categoryRows) {
                    // Find Category Name (h3)
                    Element h3 = row.select("h3").first();
                    if (h3 == null) continue;

                    Nomination.AwardCategory category = detectCategory(h3.text());
                    if (category == null) continue;

                    // A. Winner
                    Element awardWinnerDiv = row.select("div.winner").first();
                    if (awardWinnerDiv != null && !awardWinnerDiv.text().isBlank()) {
                        saveNomination(comp, awardWinnerDiv.text().trim(), category, true);

                        if (category == Nomination.AwardCategory.ROBOT_GAME) {
                            updateRobotGameRank(comp, awardWinnerDiv.text().trim(), 1);
                        }
                    }

                    // B. Nominations
                    Element nomDiv = row.select("div.col-md-5").first();
                    if (nomDiv != null) {
                        String[] lines = nomDiv.html().split("<br\\s*/?>");
                        for (String line : lines) {
                            String text = Jsoup.parse(line).text().trim();
                            // Filter out the label "Nominierungen:" or empty lines
                            if (category == Nomination.AwardCategory.CHAMPION) {
                                continue;
                            }
                            else if (category == Nomination.AwardCategory.ROBOT_GAME) {
                                // Parse "2. Platz: TeamName"
                                if (text.startsWith("2. Platz:")) {
                                    updateRobotGameRank(comp, text.replace("2. Platz:", "").trim(), 2);
                                } else if (text.startsWith("3. Platz:")) {
                                    updateRobotGameRank(comp, text.replace("3. Platz:", "").trim(), 3);
                                }
                            }
                            else if (!text.isEmpty() && !text.contains("Nominierungen:")) {
                                saveNomination(comp, text, category, false);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to scrape overview: {}", resultsUrl, e);
        }
    }

    private void savePlace(Competition comp, String teamName, int placeRank, boolean advancing) {
        findTeamByName(comp, teamName).ifPresent(team -> {
            Place place = placeRepository.findByCompetitionAndSeasonTeam(comp, team)
                .orElse(Place.builder().competition(comp).seasonTeam(team).build());
            place.setPlace(placeRank);
            place.setAdvancing(advancing);
            placeRepository.save(place);
            LOG.info("      -> Place {}: {}", placeRank, team.getName());
        });
    }

    private void saveNomination(Competition comp, String teamName, Nomination.AwardCategory category, boolean isWinner) {
        findTeamByName(comp, teamName).ifPresent(team -> {
            // We use a custom query or strict check to avoid duplicates
            Nomination nom = nominationRepository.findByCompetitionAndSeasonTeamAndCategory(
                    comp, team, category)
                .orElse(Nomination.builder()
                    .competition(comp)
                    .seasonTeam(team)
                    .category(category)
                    .isAwardWinner(isWinner)
                    .build());
            nom.setAwardWinner(isWinner); // Update in case it changed
            nominationRepository.save(nom);
        });
    }

    private Optional<SeasonTeam> findTeamByName(Competition comp, String teamName) {
        // Simple name lookup logic
        // We strip "(Winner)" or similar garbage if it exists (though parsing usually handles it)
        String cleanName = teamName.replace("(Winner)", "").trim();
        return seasonTeamRepository.findByRegisteredCompetitionsContainsAndNameIgnoreCase(comp, cleanName);
    }

    private Nomination.AwardCategory detectCategory(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("champion")) return Nomination.AwardCategory.CHAMPION;
        if (lower.contains("forschung")) return Nomination.AwardCategory.RESEARCH;
        if (lower.contains("roboterdesign")) return Nomination.AwardCategory.ROBOT_DESIGN;
        if (lower.contains("grundwerte")) return Nomination.AwardCategory.CORE_VALUES;
        if (lower.contains("coaching")) return Nomination.AwardCategory.COACHING;
        if (lower.contains("robot-game")) return Nomination.AwardCategory.ROBOT_GAME;
        return null;
    }

    private void updateRobotGameRank(Competition comp, String teamName, int rank) {
        findTeamByName(comp, teamName).ifPresent(team -> {
            // We assume the Result entry might already exist from the scoreboard scraper.
            // If not, we create it (though scores will be 0 until that scraper runs).
            RobotGameResult result = robotGameResultRepository
                .findByCompetitionAndSeasonTeam(comp, team)
                .orElse(RobotGameResult.builder()
                    .competition(comp)
                    .seasonTeam(team)
                    .build());

            result.setRank(rank);
            robotGameResultRepository.save(result);
            LOG.info("      -> Robot Game Rank {}: {}", rank, team.getName());
        });
    }
}