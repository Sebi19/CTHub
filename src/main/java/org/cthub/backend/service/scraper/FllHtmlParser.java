package org.cthub.backend.service.scraper;

import org.apache.commons.codec.digest.DigestUtils;
import org.cthub.backend.model.Competition;
import org.cthub.backend.model.Nomination;
import org.cthub.backend.model.Season;
import org.cthub.backend.service.scraper.dto.ScrapedNominationDto;
import org.cthub.backend.service.scraper.dto.ScrapedAwardsAndRanksDto;
import org.cthub.backend.service.scraper.dto.ScrapedEventDetailsDto;
import org.cthub.backend.service.scraper.dto.ScrapedEventOverviewDto;
import org.cthub.backend.service.scraper.dto.ScrapedLinkDto;
import org.cthub.backend.service.scraper.dto.ScrapedPlaceDto;
import org.cthub.backend.service.scraper.dto.ScrapedRobotGameResultsDto;
import org.cthub.backend.service.scraper.dto.ScrapedRobotGameScoreDto;
import org.cthub.backend.service.scraper.dto.ScrapedTeamDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class FllHtmlParser {
    private static final String SEASON_PART_TEMPLATE = "de/challenge-%s";
    private static final Pattern TEAM_COUNT_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*([\\d-]+)");


    public String computeOverviewHash(String html) {
        Document doc = Jsoup.parse(html);
        // We only care about the tables/lists of events.
        // If FIRST changes the header image or footer news, we don't want to re-scrape.

        StringBuilder signature = new StringBuilder();

        // 1. Regional Tables (DE, AT, CH)
        signature.append(doc.select("#challenge-table-de").outerHtml());
        signature.append(doc.select("#challenge-table-at").outerHtml());
        signature.append(doc.select("#challenge-table-ch").outerHtml());

        // 2. Qualifications & Finals (These are usually in standard content divs)
        // We look for the headers and grab the following div
        Element qualiHeader = doc.selectFirst("h4:contains(Qualifikationswettbewerbe)");
        if (qualiHeader != null && qualiHeader.nextElementSibling() != null) {
            signature.append(qualiHeader.nextElementSibling().outerHtml());
        }

        Element finalHeader = doc.selectFirst("h4:contains(Finale)");
        if (finalHeader != null && finalHeader.nextElementSibling() != null) {
            signature.append(finalHeader.nextElementSibling().outerHtml());
        }

        return DigestUtils.sha256Hex(signature.toString());
    }

    public String computeDetailHash(String html) {
        Document doc = Jsoup.parse(html);

        StringBuilder signature = new StringBuilder();
        for (Element el : doc.select("div.location-detail")) {
            signature.append(el.outerHtml());
        }

        return DigestUtils.sha256Hex(signature.toString());
    }

    public String computeRobotGameHash(String html) {
        Document doc = Jsoup.parse(html);
        // Only hash the scoreboard table to avoid noise
        Element table = doc.selectFirst("table.scoreboard");
        return table != null ? DigestUtils.sha256Hex(table.outerHtml()) : "";
    }

    public String computeAwardsHash(String html) {
        Document doc = Jsoup.parse(html);
        // Hash the main container
        Element table = doc.selectFirst("div.final-award-table");
        return table != null ? DigestUtils.sha256Hex(table.outerHtml()) : "";
    }

    public List<ScrapedEventOverviewDto> parseCompetitionsList(String html, Season season) {
        Document doc = Jsoup.parse(html);
        List<ScrapedEventOverviewDto> events = new ArrayList<>();

        // 1. Regional Tables
        events.addAll(parseRegionalTable(doc, "challenge-table-de", "DE", season));
        events.addAll(parseRegionalTable(doc, "challenge-table-at", "AT", season));
        events.addAll(parseRegionalTable(doc, "challenge-table-ch", "CH", season));

        // 2. Qualifications
        Element qualiHeader = doc.select("h4:contains(Qualifikationswettbewerbe)").first();
        if (qualiHeader != null) {
            Element qualiDiv = qualiHeader.nextElementSibling();
            if (qualiDiv != null) {
                events.addAll(parseRows(qualiDiv, null, season, Competition.CompetitionType.QUALIFICATION));
            }
        }

        // 3. Finals
        Element finalHeader = doc.select("h4:contains(Finale)").first();
        if (finalHeader != null) {
            Element finalDiv = finalHeader.nextElementSibling();
            if (finalDiv != null) {
                events.addAll(parseRows(finalDiv, null, season, Competition.CompetitionType.FINAL));
            }
        }

        return events;
    }

    public ScrapedEventDetailsDto parseEventPage(String html) {
        Document doc = Jsoup.parse(html);

        // Parse Metadata
        // Note: You might need to adjust "Veranstaltungsort" based on actual HTML labels
        String location = findMetadata(doc, "Veranstaltungsort");
        String contactName = findMetadata(doc, "Kontakt");
        String contactEmail = findEmail(doc);
        LocalDate date = parseDate(doc);
        List<ScrapedLinkDto> webLinks = findLinks(doc, "Weblinks");
        String qualificationUrlPart = null;

        List<ScrapedLinkDto> correspondingQualifications = findLinks(doc, "Zugehöriger Qualifikationswettbewerb");
        if (correspondingQualifications.size() ==  1) {
            String href = correspondingQualifications.getFirst().getUrl();
            qualificationUrlPart = href.substring(href.lastIndexOf("/") + 1);
        }


        // Check for Results
        String resultsUrlPart = null;
        // Look for "Ergebnisse" link
        List<ScrapedLinkDto> resultLinks = findLinks(doc, "Ergebnisse");
        if (!resultLinks.isEmpty()) {
            ScrapedLinkDto mainResultLink = resultLinks.getFirst();
            String href = mainResultLink.getUrl();
            resultsUrlPart = href.substring(href.lastIndexOf("/") + 1);
        }

        // Parse Teams
        List<ScrapedTeamDto> teams = parseTeamsList(doc);

        return ScrapedEventDetailsDto.builder()
            .location(location)
            .date(date)
            .contactName(contactName)
            .contactEmail(contactEmail)
            .webLinks(webLinks)
            .resultsUrlPart(resultsUrlPart)
            .qualificationUrlPart(qualificationUrlPart)
            .teams(teams)
            .build();
    }

    public ScrapedRobotGameResultsDto parseRobotGameResults(String html) {
        Document doc = Jsoup.parse(html);
        List<ScrapedRobotGameScoreDto> results = new ArrayList<>();

        Element table = doc.selectFirst("table.scoreboard");
        if (table == null) return ScrapedRobotGameResultsDto.builder().build(); // No scores yet

        // 1. MAP HEADERS (Determine which column index holds which score)
        // keys: "VR I", "VR II", "VR III", "Beste VR", "VF", "HF", "F I", "F II", "Platz"
        List<String> headers = new ArrayList<>();
        for (Element th : table.select("thead tr th")) {
            headers.add(cleanText(th.text()));
        }

        // 2. PARSE ROWS
        Elements rows = table.select("tbody tr");
        int rank = 0;
        for (Element row : rows) {
            try {
                // Find Team ID (usually in a span with class 'team-id')
                Element idSpan = row.selectFirst("span.team-id");
                if (idSpan == null) continue;

                rank += 1; // Increment rank for each team (assuming rows are sorted by rank)

                String fllId = cleanText(idSpan.text()).replaceAll("[\\[\\]()]", ""); // Strip brackets "(1234)"

                // Create Builder
                ScrapedRobotGameScoreDto.ScrapedRobotGameScoreDtoBuilder builder = ScrapedRobotGameScoreDto.builder()
                    .fllId(fllId)
                    .rank(rank);

                // Iterate cells and map to builder based on header index
                Elements cells = row.select("td");

                // Note: headers usually include the first "Team" column, so data indices might need offset.
                // Standard FLL table: Col 0 is Team Name (TH), Col 1..N are Scores (TD)
                // So header index 1 corresponds to cell index 0.

                for (int i = 0; i < cells.size(); i++) {
                    // Safety check: Don't read beyond headers
                    if (i + 1 >= headers.size()) break;

                    String header = headers.get(i + 1); // +1 because index 0 is "Team"
                    String cellText = cells.get(i).text();

                    int score = parseScore(cellText);
                    Integer scoreObj = score == 0 ? null : score;

                    switch (header) {
                        case "VR I": builder.run1(score); break;
                        case "VR II": builder.run2(score); break;
                        case "VR III": builder.run3(score); break;
                        case "Beste VR": builder.bestRun(score); break;
                        case "VF": builder.quarterFinal(scoreObj); break;
                        case "HF": builder.semiFinal(scoreObj); break;
                        case "F I": builder.final1(scoreObj); break;
                        case "F II": builder.final2(scoreObj); break;
                    }
                }

                results.add(builder.build());

            } catch (Exception e) {
                // Log and continue
            }
        }

        return ScrapedRobotGameResultsDto.builder()
            .scores(results)
            .build();
    }

    public ScrapedAwardsAndRanksDto parseAwardResults(String html) {
        Document doc = Jsoup.parse(html);
        List<ScrapedPlaceDto> places = new ArrayList<>();
        List<ScrapedNominationDto> nominations = new ArrayList<>();

        Element table = doc.selectFirst("div.final-award-table");
        if (table == null) return ScrapedAwardsAndRanksDto.builder().places(places).nominations(nominations).build();

        // --- PART A: The "Champion" Row (Overall Winner & Advancing Teams) ---
        Element championRow = table.selectFirst("div.row.champion");
        if (championRow != null) {

            // 1. Determine how many teams advance (Regex: "Platz 1 bis X qualifiziert")
            int qualifierCount = 1; // Default
            Element msg = championRow.selectFirst("div.next-round-message");
            if (msg != null) {
                Matcher m = Pattern.compile("Platz \\d+ bis (\\d+)").matcher(msg.text());
                if (m.find()) {
                    qualifierCount = Integer.parseInt(m.group(1));
                }
            }

            // 2. The Winner (Place 1)
            Element winnerDiv = championRow.selectFirst("div.winner");
            if (winnerDiv != null) {
                places.add(ScrapedPlaceDto.builder()
                    .teamName(cleanTeamName(winnerDiv.text()))
                    .place(1)
                    .advancing(qualifierCount >= 1)
                    .build());

                nominations.add(ScrapedNominationDto.builder()
                    .teamName(cleanTeamName(winnerDiv.text()))
                    .category(Nomination.AwardCategory.CHAMPION)
                    .isWinner(true)
                    .build());
            }

            // 3. The Runners Up (Place 2 & 3) & "Further Qualifiers"
            // They are usually in the col-md-5 block, separated by <br>
            Element detailsDiv = championRow.selectFirst("div.col-md-5");
            if (detailsDiv != null) {
                for (String line : detailsDiv.html().split("<br\\s*/?>")) {
                    String text = Jsoup.parse(line).text().trim(); // Clean HTML tags

                    if (text.startsWith("2. Platz:")) {
                        places.add(ScrapedPlaceDto.builder()
                            .teamName(cleanTeamName(text.replace("2. Platz:", "")))
                            .place(2)
                            .advancing(qualifierCount >= 2)
                            .build());
                    } else if (text.startsWith("3. Platz:")) {
                        places.add(ScrapedPlaceDto.builder()
                            .teamName(cleanTeamName(text.replace("3. Platz:", "")))
                            .place(3)
                            .advancing(qualifierCount >= 3)
                            .build());
                    } else if (text.startsWith("Weiterhin qualifiziert: ")) {
                        // Comma separated list of extra qualifiers
                        String[] teamNames = text.replace("Weiterhin qualifiziert: ", "").split(",");
                        int currentPlace = 4;
                        for (String name : teamNames) {
                            places.add(ScrapedPlaceDto.builder()
                                .teamName(cleanTeamName(name))
                                .place(currentPlace++)
                                .advancing(true)
                                .build());
                        }
                    }
                }
            }
        }

        // --- PART B: The Category Awards ---
        // Select all rows that are NOT header and NOT champion
        for (Element row : table.select("div.row:not(.header):not(.champion)")) {
            // Find Category
            Element h3 = row.selectFirst("h3");
            if (h3 == null) continue;

            Nomination.AwardCategory category = detectCategory(h3.text());
            if (category == null) continue;

            // 1. The Winner
            Element winnerDiv = row.selectFirst("div.winner");
            if (winnerDiv != null && !winnerDiv.text().isBlank()) {
                nominations.add(ScrapedNominationDto.builder()
                    .teamName(cleanTeamName(winnerDiv.text()))
                    .category(category)
                    .isWinner(true)
                    .build());
            }

            // 2. The Nominees (in col-md-5)
            Element nomDiv = row.selectFirst("div.col-md-5");
            if (nomDiv != null) {
                for (String line : nomDiv.html().split("<br\\s*/?>")) {
                    String text = Jsoup.parse(line).text().trim();
                    if (text.isEmpty() || text.contains("Nominierungen:")) continue;

                    // Ignore robot game (2nd/3rd place instead of nominees)
                    if (category != Nomination.AwardCategory.ROBOT_GAME) {
                        // Standard Nomination
                        nominations.add(ScrapedNominationDto.builder()
                            .teamName(cleanTeamName(text))
                            .category(category)
                            .isWinner(false)
                            .build());
                    }
                }
            }
        }

        return ScrapedAwardsAndRanksDto.builder()
            .places(places)
            .nominations(nominations)
            .build();
    }


    private List<ScrapedEventOverviewDto> parseRegionalTable(Document doc, String divId, String country, Season season) {
        Element div = doc.getElementById(divId);
        if (div != null) {
            return parseRows(div, country, season, Competition.CompetitionType.REGIONAL);
        }
        return new ArrayList<>();
    }

    private List<ScrapedEventOverviewDto> parseRows(Element container, String country, Season season, Competition.CompetitionType type) {
        List<ScrapedEventOverviewDto> results = new ArrayList<>();
        Elements rows = container.select("a.row");
        String seasonPart = getSeasonPart(season);

        for (Element row : rows) {
            String href = row.attr("href");

            // Filter: Make sure it belongs to THIS season
            if (href.contains(seasonPart)) {

                // 1. Get All Columns (Direct children only)
                Elements columns = row.select("> div");

                // Name is in 1st Column (Index 0)
                String name = columns.isEmpty() ? "Unknown" : columns.getFirst().text();

                // URL Part
                String urlPart = href.substring(href.lastIndexOf("/") + 1);

                // 2. Extract Team Counts (Usually in 3rd Column -> Index 2)
                int registered = 0;
                int max = 0;

                if (columns.size() >= 3) {
                    String rawCountText = columns.get(2).text(); // e.g., "12 / 24 Teams"
                    Matcher matcher = TEAM_COUNT_PATTERN.matcher(rawCountText);

                    if (matcher.find()) {
                        // Group 1: Registered
                        registered = Integer.parseInt(matcher.group(1));

                        // Group 2: Max (Handle "-" case)
                        String maxStr = matcher.group(2);
                        if (!maxStr.equals("-")) {
                            try {
                                max = Integer.parseInt(maxStr);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                results.add(ScrapedEventOverviewDto.builder()
                    .name(name)
                    .urlPart(urlPart)
                    .country(country)
                    .type(type)
                    .registeredTeamCount(registered)
                    .maxTeamCount(max)
                    .build());
            }
        }
        return results;
    }

    private String getSeasonPart(Season season) {
        // Same logic as before: "2024" -> "2024-25"
        int startYear = season.getStartYear();
        int endYearShort = (startYear + 1) % 100;
        String seasonPart = String.format("%d-%d", startYear, endYearShort);
        return String.format(SEASON_PART_TEMPLATE, seasonPart);
    }

    private List<ScrapedTeamDto> parseTeamsList(Document doc) {
        List<ScrapedTeamDto> teams = new ArrayList<>();
        // Selector from your screenshot/old code
        Elements rows = doc.select("div.ce_table.teams div.row:not(.header)");

        for (Element row : rows) {
            try {
                // Columns: 1=ID, 2=Name, 3=Institution, 4=City/Links
                // Using > div to be safe as discussed
                Elements cols = row.select("> div");
                if (cols.size() < 4) continue;

                String fllId = cols.get(0).text().trim();
                String teamName = cols.get(1).text().trim();
                String institution = cols.get(2).text().trim();

                // Column 4 is complex (City + Popup Links)
                Element col4 = cols.get(3);
                String city = col4.select("span.text").text().trim();
                List<ScrapedLinkDto> links = parseTeamLinks(col4);

                teams.add(ScrapedTeamDto.builder()
                    .fllId(fllId)
                    .name(teamName)
                    .institution(institution)
                    .city(city)
                    .links(links)
                    .build());
            } catch (Exception e) {
                // Log but continue
            }
        }
        return teams;
    }

    private List<ScrapedLinkDto> parseTeamLinks(Element container) {
        // The popup content is often hidden in 'data-inline-content' attribute
        Element infoButton = container.selectFirst("a.info.inline");

        if (infoButton != null) {
            String hiddenHtml = infoButton.attr("data-inline-content");
            if (!hiddenHtml.isEmpty()) {
                Document popupDoc = Jsoup.parse(hiddenHtml);
                return findLinks(popupDoc, "Social Media");
            }
        }
        return new ArrayList<>();
    }

    // --- Helpers (Ported from old Service) ---

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

    private String cleanText(String input) {
        return input == null ? null : input.replaceAll("[:\\t]+", " ").trim();
    }

    private LocalDate parseDate(Document doc) {
        String dateStr = findMetadata(doc, "Termin");
        if (dateStr != null) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            } catch (Exception ignored) {}
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

    private List<ScrapedLinkDto> findLinks(Document doc, String keyword) {
        List<ScrapedLinkDto> foundLinks = new ArrayList<>();

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
                        foundLinks.add(ScrapedLinkDto.builder()
                            .url(href)
                            .label(text.isEmpty() ? "Link" : text) // Fallback label
                            .build());
                    }
                }
            }
        }
        return foundLinks;
    }

    private int parseScore(String text) {
        if (text == null || text.trim().equals("-") || text.isEmpty()) return 0;
        try {
            // Remove non-numeric chars (except minus if needed, though scores are usually positive)
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String cleanTeamName(String input) {
        if (input == null) return "";
        // Remove "(Winner)" or other artifacts if present
        return input.replace("(Winner)", "").trim();
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
}
