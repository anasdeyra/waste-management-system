package com.ase.wastems.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues and validates the opaque bearer tokens used by the REST API.
 *
 * <p>The scaffold keeps tokens in memory: they are lost on restart and are not shared between
 * instances. Replacing this component with a signed JWT is a local change that does not affect
 * the rest of the application.
 */
@Component
public class TokenService {

    private final Map<String, TokenEntry> tokens = new ConcurrentHashMap<>();
    private final Duration ttl;

    public TokenService(@Value("${wastems.security.token-ttl-minutes:480}") long ttlMinutes) {
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    /** Creates a new token bound to the given username. */
    public String issue(String username) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new TokenEntry(username, Instant.now().plus(ttl)));
        return token;
    }

    /** Returns the username bound to a valid, non-expired token. */
    public Optional<String> resolve(String token) {
        TokenEntry entry = tokens.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            tokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(entry.username());
    }

    /** Invalidates a token, e.g. on logout. */
    public void revoke(String token) {
        tokens.remove(token);
    }

    private record TokenEntry(String username, Instant expiresAt) {}
}
