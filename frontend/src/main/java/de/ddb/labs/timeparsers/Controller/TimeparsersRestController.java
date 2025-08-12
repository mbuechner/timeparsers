package de.ddb.labs.timeparsers.Controller;

/*
 * Copyright 2023-2025 Michael Büchner, Deutsche Digitale Bibliothek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fiz.ddb.spark.transformation.util.timeparser.TimeParser;
import de.fiz.ddb.spark.transformation.util.timeparser.TimeParserNew;
import europeana.rnd.dataprocessing.dates.DatesNormaliser;
import europeana.rnd.dataprocessing.dates.Match;
import europeana.rnd.dataprocessing.dates.MatchId;
import europeana.rnd.dataprocessing.dates.edtf.EdtfSerializer;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600)
class TimeparsersRestController {

    private static final List<String> GRAIN_ORDER = List.of("year", "month", "day", "hour", "minute", "second");

    private static final Logger LOG = LoggerFactory.getLogger(TimeparsersRestController.class);
    private final DatesNormaliser edn;

    @Autowired
    private OkHttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${duckling.url}")
    private String ducklingUrl;

    public TimeparsersRestController() {
        edn = new DatesNormaliser();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() throws Exception {
        TimeParser.init("myTransformationProcessId", "myTransformationStatusId");
        TimeParserNew.init("myNewTransformationProcessId", "myNewTransformationStatusId");
    }

    @RequestMapping(
            method = RequestMethod.GET,
            produces = "application/json",
            value = "/parse"
    )
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getResource(HttpServletRequest request, @RequestParam(value = "value", required = true) String value) {

        final Instant start = Instant.now();
        LOG.info("{}: Start parsing...", value);

        final Map<String, Object> response = new HashMap<>();

        // DDB-Timeparser
        final Map<String, Object> ddbResponse = new HashMap<>();
        ddbResponse.put("status", "no_match");
        try {
            final String ddbTimeparser = TimeParser.parseTime(value, "");
            if (!ddbTimeparser.isBlank()) {
                ddbResponse.put("status", "ok");
                ddbResponse.put("value", ddbTimeparser);
            }
        } catch (Exception e) {
            LOG.error("{}: {}", value, e.getMessage());
            ddbResponse.put("status", "error");
            ddbResponse.put("message", e.getMessage());
        }

        // DDB-Timeparser NEW!
        final Map<String, Object> ddbNewResponse = new HashMap<>();
        ddbNewResponse.put("status", "no_match");
        try {
            final String ddbTimeparser = TimeParserNew.parseTime(value, "");
            if (!ddbTimeparser.isBlank()) {
                ddbNewResponse.put("status", "ok");
                ddbNewResponse.put("value", ddbTimeparser);
            }
        } catch (Exception e) {
            LOG.error("{}: {}", value, e.getMessage());
            ddbNewResponse.put("status", "error");
            ddbNewResponse.put("message", e.getMessage());
        }

        // Europeana-Timeparser
        final Map<String, Object> europeanaResponse = new HashMap<>();
        europeanaResponse.put("status", "no_match");
        try {
            final Match em = edn.normaliseDateProperty(value);

            if (em.getMatchId() != MatchId.NO_MATCH && em.getMatchId() != MatchId.INVALID) {
                final String edtfStr = EdtfSerializer.serialize(em.getExtracted().getEdtf());
                europeanaResponse.put("status", "ok");
                europeanaResponse.put("type", em.getMatchId());
                europeanaResponse.put("value", em.getMatchId() == MatchId.BcAd ? "-" + edtfStr : edtfStr);
            }
        } catch (Exception e) {
            LOG.error("{}: {}", value, e.getMessage());
            europeanaResponse.put("status", "error");
            europeanaResponse.put("message", e.getMessage());
        }

        // Duckling
        final Map<String, Object> ducklingResponse = new HashMap<>();
        ducklingResponse.put("status", "no_match");
        final String form = "locale=de_DE&dims=[\"time\"]&tz=UTC&text=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
        final RequestBody body = RequestBody.create(
                form, okhttp3.MediaType.get("application/x-www-form-urlencoded; charset=utf-8")
        );

        final String fullDucklingUrl = ducklingUrl + (ducklingUrl.endsWith("/") ? "parse" : "/parse");
        LOG.info("Start request at Duckling under {}", fullDucklingUrl);

        final Request okreq = new Request.Builder()
                .url(fullDucklingUrl)
                .post(body)
                .build();

        try (final Response okresp = httpClient.newCall(okreq).execute()) {
            if (okresp.isSuccessful()) {

                final String json = okresp.body().string();
                final JsonNode root = objectMapper.readTree(json);

                LOG.debug("Duckling says: {}", root.toPrettyString());
                ducklingResponse.put("debug", root.toPrettyString());
                for (JsonNode node : root) {
                    if (!"time".equals(node.path("dim").asText())) {
                        continue;
                    }
                    String edtf = ducklingTimeToEDTF(node);
                    if (edtf != null) {
                        ducklingResponse.put("status", "ok");
                        ducklingResponse.put("value", edtf);
                        break; // nur erstes Match
                    }
                }
            }
        } catch (Exception e) {
            ducklingResponse.put("status", "error");
            ducklingResponse.put("message", e.getMessage());
        }

        final Map<String, Object> parsers = new HashMap<>();
        parsers.put("ddb", ddbResponse);
        parsers.put("ddbNew", ddbNewResponse);
        parsers.put("europeana", europeanaResponse);
        parsers.put("duckling", ducklingResponse);

        response.put("value", value);
        response.put("parser", parsers);

        final Instant end = Instant.now();
        final Duration duration = Duration.between(start, end);
        LOG.info("{}: Parsing finshed in {} ms. ", value, duration.toMillis());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    private static String commonGrain(String g1, String g2) {
        int i1 = Math.max(0, GRAIN_ORDER.indexOf(g1));
        int i2 = Math.max(0, GRAIN_ORDER.indexOf(g2));
        return GRAIN_ORDER.get(Math.max(i1, i2)); // gröbere von beiden
    }

    private static ZonedDateTime inclusiveEnd(ZonedDateTime toExclusive, String grain) {
        return switch (grain) {
            case "year" ->
                toExclusive.minusYears(1);
            case "month" ->
                toExclusive.minusMonths(1);
            case "day" ->
                toExclusive.minusDays(1);
            case "hour" ->
                toExclusive.minusHours(1);
            case "minute" ->
                toExclusive.minusMinutes(1);
            case "second" ->
                toExclusive.minusSeconds(1);
            default ->
                toExclusive; // Fallback
        };
    }

    private static String formatEDTF(ZonedDateTime zdt, String grain) {
        return switch (grain) {
            case "year" ->
                String.format("%04d", zdt.getYear());
            case "month" ->
                zdt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case "day" ->
                zdt.format(DateTimeFormatter.ISO_LOCAL_DATE);
            case "hour" ->
                zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00:00"));
            case "minute" ->
                zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:00"));
            case "second" ->
                zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            default ->
                zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        };
    }

// Holt "from"/"to" entweder direkt oder aus values[0]
    private static JsonNode pickNode(JsonNode valueNode, String key) {
        if (valueNode.hasNonNull(key)) {
            return valueNode.path(key);
        }
        JsonNode alt = valueNode.path("values").path(0).path(key);
        return alt.isMissingNode() ? null : alt;
    }

    private static String toEdtfFromValue(JsonNode valueNode) {
        String grain = valueNode.path("grain").asText("day");
        ZonedDateTime zdt = ZonedDateTime.parse(valueNode.path("value").asText());
        return formatEDTF(zdt, grain);
    }

    private static String toEdtfFromInterval(JsonNode valueNode) {
        JsonNode fromN = pickNode(valueNode, "from");
        JsonNode toN = pickNode(valueNode, "to");

        // nur FROM (z. B. "ab 1858")
        if (fromN != null && (toN == null || toN.isNull())) {
            String grain = fromN.path("grain").asText("day");
            ZonedDateTime from = ZonedDateTime.parse(fromN.path("value").asText());
            return formatEDTF(from, grain) + "/..";
        }

        // nur TO (z. B. "bis 1858")
        if (toN != null && (fromN == null || fromN.isNull())) {
            String grain = toN.path("grain").asText("day");
            ZonedDateTime toEx = ZonedDateTime.parse(toN.path("value").asText());
            ZonedDateTime toIn = inclusiveEnd(toEx, grain);
            return "../" + formatEDTF(toIn, grain);
        }

        // FROM & TO
        String grainFrom = fromN.path("grain").asText("day");
        String grainTo = toN.path("grain").asText("day");
        String grain = commonGrain(grainFrom, grainTo);

        ZonedDateTime from = ZonedDateTime.parse(fromN.path("value").asText());
        ZonedDateTime toEx = ZonedDateTime.parse(toN.path("value").asText());
        ZonedDateTime toIn = inclusiveEnd(toEx, grain);

        String start = formatEDTF(from, grain);
        String end = formatEDTF(toIn, grain);
        return start + "/" + end;
    }

// Dispatcher: wandelt ein einzelnes Duckling-"time"-Ergebnis in EDTF
    public static String ducklingTimeToEDTF(JsonNode timeNode) {
        JsonNode value = timeNode.path("value");
        String type = value.path("type").asText();
        return switch (type) {
            case "value" ->
                toEdtfFromValue(value);
            case "interval" ->
                toEdtfFromInterval(value);
            default ->
                null; // andere Typen ignorieren
        };
    }
}
