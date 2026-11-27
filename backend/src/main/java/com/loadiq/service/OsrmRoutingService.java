
package com.loadiq.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OsrmRoutingService {

    private static final String OSRM_ROUTE_URL =
            "https://router.project-osrm.org/route/v1/driving/";

    private static final String OSRM_TRIP_URL =
            "https://router.project-osrm.org/trip/v1/driving/";

    private static final ParameterizedTypeReference<Map<String, Object>>
            RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getRoute(
            double startLongitude,
            double startLatitude,
            double destinationLongitude,
            double destinationLatitude) {

        validateCoordinates(startLongitude, startLatitude);
        validateCoordinates(destinationLongitude, destinationLatitude);

        String coordinates =
                startLongitude + "," + startLatitude + ";"
                + destinationLongitude + "," + destinationLatitude;

        String url = UriComponentsBuilder
                .fromUriString(OSRM_ROUTE_URL + coordinates)
                .queryParam("overview", "full")
                .queryParam("geometries", "geojson")
                .queryParam("steps", "false")
                .build()
                .toUriString();

        Map<String, Object> response = fetchResponse(url);

        if (response == null || !"Ok".equals(response.get("code"))) {
            throw new IllegalStateException(
                    "Unable to find a road route."
            );
        }

        List<Map<String, Object>> routes =
                getMapList(response, "routes");

        if (routes.isEmpty()) {
            throw new IllegalStateException("No route found.");
        }

        return routes.get(0);
    }

    public Map<String, Object> getOptimizedTrip(
            List<Coordinate> coordinatesList) {

        if (coordinatesList == null || coordinatesList.size() < 2) {
            throw new IllegalArgumentException(
                    "At least two locations are required."
            );
        }

        if (coordinatesList.size() > 10) {
            throw new IllegalArgumentException(
                    "A maximum of 10 locations is allowed per request."
            );
        }

        StringBuilder coordinates = new StringBuilder();

        for (int i = 0; i < coordinatesList.size(); i++) {

            Coordinate coordinate = coordinatesList.get(i);

            if (coordinate == null) {
                throw new IllegalArgumentException(
                        "Location coordinates cannot be empty."
                );
            }

            validateCoordinates(
                    coordinate.longitude(),
                    coordinate.latitude()
            );

            if (i > 0) {
                coordinates.append(";");
            }

            coordinates
                    .append(coordinate.longitude())
                    .append(",")
                    .append(coordinate.latitude());
        }

        String url = UriComponentsBuilder
                .fromUriString(OSRM_TRIP_URL + coordinates)
                .queryParam("roundtrip", "false")
                .queryParam("source", "first")
                .queryParam("destination", "any")
                .queryParam("overview", "full")
                .queryParam("geometries", "geojson")
                .queryParam("steps", "false")
                .build()
                .toUriString();

        Map<String, Object> response = fetchResponse(url);

        if (response == null || !"Ok".equals(response.get("code"))) {
            throw new IllegalStateException(
                    "Unable to optimize the delivery route."
            );
        }

        List<Map<String, Object>> trips =
                getMapList(response, "trips");

        if (trips.isEmpty()) {
            throw new IllegalStateException(
                    "No optimized delivery route was found."
            );
        }

        return response;
    }

    // Fetch OSRM response using a parameterized generic type.
    private Map<String, Object> fetchResponse(String url) {

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                RESPONSE_TYPE
        ).getBody();
    }

    // Safely extract a list of maps without unchecked casts.
    private List<Map<String, Object>> getMapList(
            Map<String, Object> response,
            String key) {

        Object value = response.get(key);

        if (!(value instanceof List<?> items)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object item : items) {

            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }

            Map<String, Object> typedMap = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {

                if (entry.getKey() instanceof String mapKey) {
                    typedMap.put(mapKey, entry.getValue());
                }
            }

            result.add(typedMap);
        }

        return result;
    }

    private void validateCoordinates(
            double longitude,
            double latitude) {

        if (!Double.isFinite(longitude)
                || !Double.isFinite(latitude)
                || longitude < -180
                || longitude > 180
                || latitude < -90
                || latitude > 90) {

            throw new IllegalArgumentException(
                    "Invalid geographic coordinates."
            );
        }
    }

    public record Coordinate(
            double latitude,
            double longitude
    ) {}
}
