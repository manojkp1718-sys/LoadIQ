package com.loadiq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class GeocodingService {

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search";

    private static final long MIN_REQUEST_INTERVAL_MS = 1100;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private long lastRequestTime = 0;

    public GeocodingService() {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Geocodes a destination address.
     *
     * Returns null when no reliable result is available.
     */
    public synchronized Coordinate geocode(String destination) {

        if (destination == null || destination.isBlank()) {
            return null;
        }

        try {
            waitForRateLimit();

           String query = destination.trim() + ", India";
            URI uri = UriComponentsBuilder
                    .fromUriString(NOMINATIM_URL)
                    .queryParam("q", query)
                    .queryParam("format", "jsonv2")
                    .queryParam("limit", 1)
                    .queryParam("countrycodes", "in")
                    .build()
                    .encode()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();

            // Identify the application as required by Nominatim.
            headers.set(
                    HttpHeaders.USER_AGENT,
                    "LoadIQ/1.0 (logistics delivery route optimization)"
            );

            headers.set(
                    HttpHeaders.ACCEPT_LANGUAGE,
                    "en"
            );

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            uri,
                            HttpMethod.GET,
                            entity,
                            String.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {
                return null;
            }

            JsonNode results =
                    objectMapper.readTree(response.getBody());

            if (!results.isArray() || results.isEmpty()) {
                return null;
            }

            JsonNode firstResult = results.get(0);

            double latitude =
                    firstResult.path("lat").asDouble(Double.NaN);

            double longitude =
                    firstResult.path("lon").asDouble(Double.NaN);

            if (!Double.isFinite(latitude)
                    || !Double.isFinite(longitude)
                    || latitude < -90
                    || latitude > 90
                    || longitude < -180
                    || longitude > 180) {
                return null;
            }

            return new Coordinate(latitude, longitude);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.err.println(
                    "Geocoding interrupted: " + e.getMessage()
            );

            return null;

        } catch (Exception e) {

            System.err.println(
                    "Geocoding failed for "
                            + destination + ": " + e.getMessage()
            );

            return null;
        }
    }

    private void waitForRateLimit() throws InterruptedException {

        long now = System.currentTimeMillis();

        long elapsed = now - lastRequestTime;

        long waitTime = MIN_REQUEST_INTERVAL_MS - elapsed;

        if (waitTime > 0) {
            Thread.sleep(waitTime);
        }

        lastRequestTime = System.currentTimeMillis();
    }

    public record Coordinate(
            double latitude,
            double longitude
    ) {}
}