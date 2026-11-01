package com.ase.wastems.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AppUserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The real cookie-to-header CSRF flow. It needs a fresh context because the {@code csrf()} test
 * post-processor permanently swaps the token repository of the shared filter instance.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CsrfCookieFlowTest {

    @Autowired private MockMvc mvc;
    @Autowired private AppUserRepository users;
    @Autowired private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        users.save(new AppUser("admin1", encoder.encode("secret123"), AppRole.ADMIN));
    }

    @Test
    void cookieTokenSentAsHeaderIsAccepted() throws Exception {
        MvcResult r =
                mvc.perform(get("/api/auth/csrf"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                        .andExpect(cookie().exists("XSRF-TOKEN"))
                        .andReturn();
        Cookie c = r.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(c);
        String body = "{\"username\":\"admin1\",\"password\":\"secret123\"}";
        mvc.perform(
                        post("/api/auth/login")
                                .cookie(c)
                                .header("X-XSRF-TOKEN", c.getValue())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }
}
