package org.cthub.backend.auth.attempt;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.cthub.backend.mail.MailService;
import org.cthub.backend.mail.model.MailTemplate;
import org.cthub.backend.mail.render.LoginLinkModel;
import org.cthub.backend.mail.render.MailTemplateModel;
import org.cthub.backend.model.User;
import org.cthub.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The number-matching challenge end to end through the HTTP API: browser A holds the attempt
 * cookie, the "other browser" only has the e-mailed token.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LoginChallengeFlowTest {

    private static final String ATTEMPT_COOKIE = "ct_attempt";
    private static final String REFRESH_COOKIE = "ct_refresh";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @MockitoBean
    private MailService mailService;

    private record BrowserA(Cookie attempt, int challengeNumber, String linkToken) {
    }

    @Test
    void unknownAndPasswordlessAddressesGetTheSameAnswerAndNoAccountYet() throws Exception {
        String unknown = email();
        String existing = email();
        userRepository.save(User.builder().email(existing).enabled(true).role(User.Role.USER)
            .emailVerifiedAt(java.time.Instant.now()).build());

        for (String address : List.of(unknown, existing)) {
            mvc.perform(json(post("/api/auth/identify"), "{\"email\":\"" + address + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.next").value("CHALLENGE"))
                .andExpect(jsonPath("$.passwordAvailable").value(false))
                .andExpect(jsonPath("$.attempt.state").value("WAITING"))
                .andExpect(jsonPath("$.attempt.purpose").value("LOGIN"));
        }
        assertThat(userRepository.findByEmail(unknown)).isEmpty();
    }

    @Test
    void sameBrowserClickSignsInWithoutTheNumberAndCreatesTheAccount() throws Exception {
        String address = email();
        BrowserA a = start(address);

        mvc.perform(get("/api/auth/link").param("token", a.linkToken()).cookie(a.attempt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kind").value("SAME_BROWSER"));
        // Viewing changes nothing (scanners prefetch links).
        mvc.perform(get("/api/auth/attempts/current").cookie(a.attempt()))
            .andExpect(jsonPath("$.state").value("WAITING"));

        MvcResult result = mvc.perform(json(post("/api/auth/link/complete"), token(a)).cookie(a.attempt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.purpose").value("LOGIN"))
            .andExpect(jsonPath("$.user.email").value(address))
            .andExpect(jsonPath("$.user.profileComplete").value(false))
            .andReturn();
        assertThat(setCookie(result, REFRESH_COOKIE)).isNotBlank();
        assertThat(userRepository.findByEmail(address)).isPresent();
    }

    @Test
    void crossBrowserApprovalWithTheRightNumberSignsInBrowserA() throws Exception {
        BrowserA a = start(email());

        String view = mvc.perform(get("/api/auth/link").param("token", a.linkToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kind").value("CROSS"))
            .andReturn().getResponse().getContentAsString();
        List<Integer> options = JsonPath.read(view, "$.options");
        assertThat(options).hasSize(3).contains(a.challengeNumber());

        // Polling never signs anybody in.
        MvcResult poll = mvc.perform(get("/api/auth/attempts/current").cookie(a.attempt()))
            .andExpect(jsonPath("$.state").value("WAITING")).andReturn();
        assertThat(setCookie(poll, REFRESH_COOKIE)).isNull();

        MvcResult approval = mvc.perform(json(post("/api/auth/link/approve"),
                "{\"token\":\"" + a.linkToken() + "\",\"number\":" + a.challengeNumber() + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("APPROVED"))
            .andReturn();
        assertThat(setCookie(approval, REFRESH_COOKIE)).as("the approving browser stays signed out").isNull();

        mvc.perform(get("/api/auth/attempts/current").cookie(a.attempt()))
            .andExpect(jsonPath("$.state").value("APPROVED"));
        MvcResult completed = mvc.perform(post("/api/auth/attempts/current/complete").cookie(a.attempt()))
            .andExpect(status().isOk())
            .andReturn();
        assertThat(setCookie(completed, REFRESH_COOKIE)).isNotBlank();

        // The approval is spent.
        mvc.perform(post("/api/auth/attempts/current/complete").cookie(a.attempt()))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void aWrongNumberKillsTheAttemptEverywhere() throws Exception {
        BrowserA a = start(email());
        int wrong = (a.challengeNumber() + 1) % 100;

        mvc.perform(json(post("/api/auth/link/approve"),
                "{\"token\":\"" + a.linkToken() + "\",\"number\":" + wrong + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("REJECTED"));

        mvc.perform(get("/api/auth/attempts/current").cookie(a.attempt()))
            .andExpect(jsonPath("$.state").value("REJECTED"));
        mvc.perform(post("/api/auth/attempts/current/complete").cookie(a.attempt()))
            .andExpect(status().is4xxClientError());
        mvc.perform(json(post("/api/auth/link/approve"),
                "{\"token\":\"" + a.linkToken() + "\",\"number\":" + a.challengeNumber() + "}"))
            .andExpect(status().is4xxClientError());
        mvc.perform(get("/api/auth/link").param("token", a.linkToken()))
            .andExpect(jsonPath("$.kind").value("INVALID"));
    }

    @Test
    void signingInOnTheLinkBrowserInsteadCancelsBrowserA() throws Exception {
        BrowserA a = start(email());

        MvcResult claimed = mvc.perform(json(post("/api/auth/link/claim-here"), token(a)))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(setCookie(claimed, REFRESH_COOKIE)).isNotBlank();
        mvc.perform(get("/api/auth/attempts/current").cookie(a.attempt()))
            .andExpect(jsonPath("$.state").value("CLAIMED_ELSEWHERE"));
        mvc.perform(post("/api/auth/attempts/current/complete").cookie(a.attempt()))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void theLinkCannotBeCompletedFromAnotherBrowserWithoutTheNumber() throws Exception {
        BrowserA a = start(email());

        mvc.perform(json(post("/api/auth/link/complete"), token(a)))
            .andExpect(status().isForbidden());
        mvc.perform(json(post("/api/auth/link/complete"), token(a)).cookie(new Cookie(ATTEMPT_COOKIE, "forged")))
            .andExpect(status().isForbidden());
    }

    // --- helpers ----------------------------------------------------------------------------

    private BrowserA start(String address) throws Exception {
        MvcResult result = mvc.perform(json(post("/api/auth/identify"), "{\"email\":\"" + address + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.next").value("CHALLENGE"))
            .andReturn();
        String attempt = setCookie(result, ATTEMPT_COOKIE);
        assertThat(attempt).isNotBlank();
        int number = JsonPath.read(result.getResponse().getContentAsString(), "$.attempt.challengeNumber");
        return new BrowserA(new Cookie(ATTEMPT_COOKIE, attempt), number, lastLinkToken(address));
    }

    private String lastLinkToken(String address) {
        ArgumentCaptor<MailTemplateModel> model = ArgumentCaptor.forClass(MailTemplateModel.class);
        ArgumentCaptor<org.cthub.backend.mail.model.MailAddress> to =
            ArgumentCaptor.forClass(org.cthub.backend.mail.model.MailAddress.class);
        verify(mailService, atLeastOnce()).enqueue(eq(MailTemplate.LOGIN_LINK), to.capture(), model.capture(),
            any(Locale.class));
        for (int i = model.getAllValues().size() - 1; i >= 0; i--) {
            if (to.getAllValues().get(i).email().equals(address)) {
                String url = ((LoginLinkModel) model.getAllValues().get(i)).loginUrl();
                return url.substring(url.indexOf("token=") + "token=".length());
            }
        }
        throw new AssertionError("No login link was mailed to " + address);
    }

    private static String token(BrowserA a) {
        return "{\"token\":\"" + a.linkToken() + "\"}";
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String setCookie(MvcResult result, String name) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
            .filter(header -> header.startsWith(name + "="))
            .map(header -> header.substring(name.length() + 1, header.indexOf(';')))
            .filter(value -> !value.isEmpty())
            .reduce((first, second) -> second)
            .orElse(null);
    }

    private static String email() {
        return "flow-" + UUID.randomUUID() + "@flow-test.invalid";
    }
}
