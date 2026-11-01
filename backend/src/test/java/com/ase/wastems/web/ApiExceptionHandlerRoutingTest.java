package com.ase.wastems.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ase.wastems.routing.RoutingUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionHandlerRoutingTest {

    @Test
    void routingUnavailableIsMappedTo503() {
        var response = new ApiExceptionHandler()
                .routingUnavailable(new RoutingUnavailableException("Road snapshot checksum mismatch"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().message()).startsWith("Road routing is unavailable:")
                .contains("checksum mismatch");
    }
}
