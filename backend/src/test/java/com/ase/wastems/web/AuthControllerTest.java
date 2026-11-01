package com.ase.wastems.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests of the session authentication endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String BODY = "{\"username\":\"%s\",\"password\":\"%s\"}";

    @Autowired private MockMvc mvc;
    @Autowired private AppUserRepository users;
    @Autowired private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        users.save(new AppUser("admin1", encoder.encode("secret123"), AppRole.ADMIN));
    }

    private MvcResult login(String user, String pass) throws Exception {
        return mvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY.formatted(user, pass)))
                .andReturn();
    }

    @Test
    void loginWithValidCredentialsReturnsCurrentUserAndSession() throws Exception {
        MvcResult r = login("admin1", "secret123");
        assertEquals(200, r.getResponse().getStatus());
        String body = r.getResponse().getContentAsString();
        assertFalse(body.contains("$2"));
        assertFalse(body.contains("password"));
        MockHttpSession session = (MockHttpSession) r.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin1"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginIsCaseInsensitiveOnUsername() throws Exception {
        assertEquals(200, login("ADMIN1", "secret123").getResponse().getStatus());
    }

    @Test
    void wrongPasswordAndUnknownUserReturnSameGenericError() throws Exception {
        MvcResult wrong = login("admin1", "nope");
        MvcResult unknown = login("ghost", "nope");
        assertEquals(401, wrong.getResponse().getStatus());
        assertEquals(401, unknown.getResponse().getStatus());
        assertTrue(wrong.getResponse().getContentAsString().contains("Invalid username or password."));
        assertEquals(
                wrong.getResponse().getContentAsString(), unknown.getResponse().getContentAsString());
    }

    @Test
    void deactivatedAccountCannotLogin() throws Exception {
        AppUser u = users.findByUsername("admin1").orElseThrow();
        u.setActive(false);
        users.save(u);
        MvcResult r = login("admin1", "secret123");
        assertEquals(401, r.getResponse().getStatus());
        assertTrue(r.getResponse().getContentAsString().contains("Invalid username or password."));
    }

    @Test
    void loginWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY.formatted("admin1", "secret123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void meWithoutSessionIsUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session =
                (MockHttpSession) login("admin1", "secret123").getRequest().getSession(false);
        mvc.perform(post("/api/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void deactivationDuringSessionRejectsNextRequest() throws Exception {
        MockHttpSession session =
                (MockHttpSession) login("admin1", "secret123").getRequest().getSession(false);
        AppUser u = users.findByUsername("admin1").orElseThrow();
        u.setActive(false);
        users.save(u);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void passwordIsStoredAsBcryptHash() {
        String hash = users.findByUsername("admin1").orElseThrow().getPasswordHash();
        assertTrue(hash.startsWith("$2"));
        assertTrue(encoder.matches("secret123", hash));
    }

    @Test
    void blankCredentialsReturn400() throws Exception {
        mvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY.formatted("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }
}
