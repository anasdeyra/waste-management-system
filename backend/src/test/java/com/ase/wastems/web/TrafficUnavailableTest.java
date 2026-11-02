package com.ase.wastems.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AppUserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "wastems.traffic.snapshot-location=classpath:traffic/missing.json")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrafficUnavailableTest {

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        users.save(new AppUser("planner1", "x", AppRole.PLANNER));
    }

    @AfterEach
    void tearDown() {
        users.deleteAll();
    }

    @Test
    void missingSnapshotIsReportedNotFailed() throws Exception {
        mvc.perform(get("/api/traffic/overlay").with(user("planner1").roles("PLANNER"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.unavailableReason", Matchers.startsWith("Traffic snapshot not found")))
                .andExpect(jsonPath("$.links.length()").value(0))
                .andExpect(jsonPath("$.counts.total").value(0))
                .andExpect(jsonPath("$.coverageNote").exists())
                .andExpect(jsonPath("$.snapshotId").doesNotExist());
    }
}
