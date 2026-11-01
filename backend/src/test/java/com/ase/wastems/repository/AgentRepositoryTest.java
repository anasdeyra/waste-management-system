package com.ase.wastems.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.DataOrigin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** Persistence tests of {@link AgentRepository}. */
@DataJpaTest
@ActiveProfiles("test")
class AgentRepositoryTest {

    @Autowired private AgentRepository agents;
    @Autowired private AppUserRepository users;

    @Test
    void findsAgentByLinkedAccount() {
        AppUser u = users.save(new AppUser("agent1", "x", AppRole.AGENT));
        Agent a = new Agent("Maria", DataOrigin.USER_ENTERED);
        a.linkUserAccount(u);
        agents.saveAndFlush(a);
        assertEquals("Maria", agents.findByUserAccount(u).orElseThrow().getDisplayName());
    }

    @Test
    void sameAccountCannotBeLinkedTwice() {
        AppUser u = users.save(new AppUser("agent1", "x", AppRole.AGENT));
        Agent a = new Agent("Maria", DataOrigin.USER_ENTERED);
        a.linkUserAccount(u);
        agents.saveAndFlush(a);
        Agent b = new Agent("James", DataOrigin.USER_ENTERED);
        b.linkUserAccount(u);
        assertThrows(DataIntegrityViolationException.class, () -> agents.saveAndFlush(b));
    }
}
