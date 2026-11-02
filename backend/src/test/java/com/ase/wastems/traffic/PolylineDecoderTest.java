package com.ase.wastems.traffic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolylineDecoderTest {

    @Test
    void decodesAShortPolyline() {
        List<double[]> pts = PolylineDecoder.decode("uxmwFn_tbMgAwBi@eBc@sB[qC[wE");
        assertThat(pts).hasSize(6);
        assertThat(pts.get(0)[0]).isCloseTo(40.70811, within(1e-9));
        assertThat(pts.get(0)[1]).isCloseTo(-73.99944, within(1e-9));
        assertThat(pts.get(5)[0]).isCloseTo(40.70914, within(1e-9));
        assertThat(pts.get(5)[1]).isCloseTo(-73.99594, within(1e-9));
    }

    @Test
    void decodesTheCommittedRecordWithDoubledBackslashes() throws Exception {
        JsonNode root;
        try (InputStream in = getClass().getResourceAsStream("/traffic/mn01-traffic-links.json")) {
            root = new ObjectMapper().readTree(in);
        }
        String poly = null;
        for (JsonNode r : root.get("records")) {
            if ("4616333".equals(r.get("link_id").asText())) {
                poly = r.get("encoded_poly_line").asText();
            }
        }
        assertThat(poly).contains("\\\\");
        List<double[]> pts = PolylineDecoder.decode(poly);
        assertThat(pts).hasSize(49);
        assertThat(pts.get(0)[0]).isCloseTo(40.73697, within(1e-9));
        assertThat(pts.get(0)[1]).isCloseTo(-73.97477, within(1e-9));
        assertThat(pts.get(48)[0]).isCloseTo(40.70744, within(1e-9));
        assertThat(pts.get(48)[1]).isCloseTo(-73.99483, within(1e-9));
    }

    @Test
    void rejectsTruncatedOrInvalidInput() {
        assertThatThrownBy(() -> PolylineDecoder.decode("uxmwFn_tbMgA")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PolylineDecoder.decode("uxm wF")).isInstanceOf(IllegalArgumentException.class);
    }
}
