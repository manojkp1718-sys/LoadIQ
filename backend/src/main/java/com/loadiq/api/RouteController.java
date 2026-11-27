
package com.loadiq.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadiq.algorithm.RouteOptimizer;
import com.loadiq.config.DatabaseConnection;
import com.loadiq.service.OsrmRoutingService;
import com.loadiq.service.OsrmRoutingService.Coordinate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
public class RouteController {

    private static final Logger logger =
            LoggerFactory.getLogger(RouteController.class);

    private final OsrmRoutingService osrmRoutingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RouteController(OsrmRoutingService osrmRoutingService) {
        this.osrmRoutingService = osrmRoutingService;
    }

    // =====================================================
    // DIJKSTRA SHORTEST ROUTE
    // =====================================================

    @PostMapping("/shortest")
    public RouteResponse findShortestRoute(
            @RequestBody RouteRequest request) {

        if (request == null
                || request.start() == null
                || request.start().isBlank()
                || request.destination() == null
                || request.destination().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Starting location and destination are required."
            );
        }

        RouteOptimizer optimizer = new RouteOptimizer();
        optimizer.loadSampleRoutes();

        List<String> route = optimizer.findShortestRoute(
                request.start().trim(),
                request.destination().trim()
        );

        if (route.isEmpty()) {
            return new RouteResponse(
                    false,
                    request.start(),
                    request.destination(),
                    List.of(),
                    null,
                    "No route found between these locations."
            );
        }

        double distance = optimizer.getShortestDistance(
                request.start().trim(),
                request.destination().trim()
        );

        return new RouteResponse(
                true,
                request.start(),
                request.destination(),
                route,
                distance,
                "Shortest route found successfully."
        );
    }

    // =====================================================
    // REAL-ROAD ROUTING USING OSRM
    // =====================================================

    @PostMapping("/road")
    public RoadRouteResponse findRoadRoute(
            @RequestBody RoadRouteRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Route request is required."
            );
        }

        try {
            validateCoordinates(
                    request.startLongitude(),
                    request.startLatitude()
            );

            validateCoordinates(
                    request.destinationLongitude(),
                    request.destinationLatitude()
            );

            Map<String, Object> route =
                    osrmRoutingService.getRoute(
                            request.startLongitude(),
                            request.startLatitude(),
                            request.destinationLongitude(),
                            request.destinationLatitude()
                    );

            double distanceMeters =
                    ((Number) route.get("distance")).doubleValue();

            double durationSeconds =
                    ((Number) route.get("duration")).doubleValue();

            return new RoadRouteResponse(
                    true,
                    distanceMeters / 1000.0,
                    durationSeconds / 60.0,
                    route.get("geometry"),
                    "Real road route found successfully."
            );

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );

        } catch (Exception e) {
            logger.error("Road routing failed", e);

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Road routing service is unavailable."
            );
        }
    }

    // =====================================================
    // MULTI-STOP ROUTE OPTIMIZATION AND HISTORY SAVING
    // =====================================================

    @PostMapping("/multi-stop")
    public MultiStopRouteResponse optimizeMultipleStops(
            @RequestBody MultiStopRouteRequest request) {

        try {
            validateMultiStopRequest(request);

            List<Coordinate> coordinates = new ArrayList<>();

            coordinates.add(new Coordinate(
                    request.startLatitude(),
                    request.startLongitude()
            ));

            for (DeliveryStop stop : request.stops()) {

                if (stop == null
                        || stop.name() == null
                        || stop.name().isBlank()) {

                    throw new IllegalArgumentException(
                            "Every delivery stop must have a name."
                    );
                }

                validateCoordinates(
                        stop.longitude(),
                        stop.latitude()
                );

                coordinates.add(new Coordinate(
                        stop.latitude(),
                        stop.longitude()
                ));
            }

            Map<String, Object> response =
                    osrmRoutingService.getOptimizedTrip(coordinates);

            List<Map<String, Object>> trips =
                    getMapList(response, "trips");

            List<Map<String, Object>> waypoints =
                    getMapList(response, "waypoints");

            if (trips.isEmpty()
                    || waypoints.isEmpty()
                    || waypoints.size() != coordinates.size()) {

                throw new IllegalStateException(
                        "Routing service returned incomplete route information."
                );
            }

            Map<String, Object> trip = trips.get(0);

            double distanceMeters =
                    ((Number) trip.get("distance")).doubleValue();

            double durationSeconds =
                    ((Number) trip.get("duration")).doubleValue();

            List<StopResult> orderedStops = new ArrayList<>();

            for (int i = 0; i < waypoints.size(); i++) {

                Map<String, Object> waypoint = waypoints.get(i);

                int optimizedIndex =
                        ((Number) waypoint.get("waypoint_index")).intValue();

                String name = i == 0
                        ? request.startName().trim()
                        : request.stops().get(i - 1).name().trim();

                orderedStops.add(new StopResult(
                        optimizedIndex + 1,
                        name,
                        coordinates.get(i).latitude(),
                        coordinates.get(i).longitude()
                ));
            }

            orderedStops.sort(
                    Comparator.comparingInt(StopResult::sequence)
            );

            double distanceKm = distanceMeters / 1000.0;
            double durationMinutes = durationSeconds / 60.0;

            Object geometry = trip.get("geometry");

            Long routeId = saveRouteHistory(
                    request,
                    orderedStops,
                    geometry,
                    distanceKm,
                    durationMinutes
            );

            return new MultiStopRouteResponse(
                    true,
                    routeId,
                    orderedStops,
                    distanceKm,
                    durationMinutes,
                    geometry,
                    "Multi-stop delivery route optimized and saved successfully."
            );

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );

        } catch (JsonProcessingException e) {
            logger.error("Unable to serialize route history", e);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to serialize route history."
            );

        } catch (SQLException e) {
            logger.error("Unable to save route history", e);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Route calculated but could not be saved in MySQL."
            );

        } catch (Exception e) {
            logger.error("Multi-stop route optimization failed", e);

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Multi-stop route optimization failed."
            );
        }
    }

    // =====================================================
    // SAFELY EXTRACT A LIST OF MAPS FROM AN OSRM RESPONSE
    // =====================================================

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

    // =====================================================
    // VALIDATE MULTI-STOP REQUEST
    // =====================================================

    private void validateMultiStopRequest(
            MultiStopRouteRequest request) {

        if (request == null
                || request.startName() == null
                || request.startName().isBlank()) {

            throw new IllegalArgumentException(
                    "Starting location name is required."
            );
        }

        if (request.stops() == null || request.stops().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one delivery stop is required."
            );
        }

        if (request.stops().size() > 9) {
            throw new IllegalArgumentException(
                    "A maximum of 9 delivery stops is allowed."
            );
        }

        validateCoordinates(
                request.startLongitude(),
                request.startLatitude()
        );
    }

    // =====================================================
    // VALIDATE GEOGRAPHIC COORDINATES
    // =====================================================

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

    // =====================================================
    // SAVE ROUTE HISTORY IN MYSQL
    // =====================================================

    private Long saveRouteHistory(
            MultiStopRouteRequest request,
            List<StopResult> optimizedStops,
            Object geometry,
            double distanceKm,
            double durationMinutes)
            throws SQLException, JsonProcessingException {

        String sql = """
                INSERT INTO route_history (
                    start_location,
                    start_latitude,
                    start_longitude,
                    selected_order_ids,
                    optimized_stops,
                    route_geometry,
                    total_distance_km,
                    estimated_duration_minutes,
                    routing_engine
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        List<Long> orderIds = request.selectedOrderIds() == null
                ? List.of()
                : request.selectedOrderIds();

        String ordersJson =
                objectMapper.writeValueAsString(orderIds);

        String stopsJson =
                objectMapper.writeValueAsString(optimizedStops);

        String geometryJson =
                objectMapper.writeValueAsString(geometry);

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, request.startName().trim());
            statement.setDouble(2, request.startLatitude());
            statement.setDouble(3, request.startLongitude());
            statement.setString(4, ordersJson);
            statement.setString(5, stopsJson);
            statement.setString(6, geometryJson);
            statement.setDouble(7, distanceKm);
            statement.setDouble(8, durationMinutes);
            statement.setString(9, "OSRM");

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException(
                        "Route history could not be inserted."
                );
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        throw new SQLException("Unable to retrieve generated route ID.");
    }

    // =====================================================
    // RETRIEVE ALL SAVED ROUTES
    // =====================================================

    @GetMapping("/history")
    public List<Map<String, Object>> getRouteHistory() {

        String sql = """
                SELECT
                    route_id,
                    start_location,
                    start_latitude,
                    start_longitude,
                    selected_order_ids,
                    optimized_stops,
                    total_distance_km,
                    estimated_duration_minutes,
                    routing_engine,
                    created_at
                FROM route_history
                ORDER BY created_at DESC
                """;

        List<Map<String, Object>> history = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            ResultSetMetaData metadata = resultSet.getMetaData();
            int columnCount = metadata.getColumnCount();

            while (resultSet.next()) {

                Map<String, Object> row = new LinkedHashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    row.put(
                            metadata.getColumnLabel(i),
                            resultSet.getObject(i)
                    );
                }

                history.add(row);
            }

            return history;

        } catch (SQLException e) {
            logger.error("Unable to retrieve route history", e);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve route history."
            );
        }
    }

    // =====================================================
    // RETRIEVE ONE ROUTE INCLUDING ITS GEOMETRY
    // =====================================================

    @GetMapping("/history/{routeId}")
    public Map<String, Object> getRouteById(
            @PathVariable Long routeId) {

        if (routeId == null || routeId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid route ID."
            );
        }

        String sql = """
                SELECT *
                FROM route_history
                WHERE route_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, routeId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (!resultSet.next()) {
                    throw new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Route history record not found."
                    );
                }

                Map<String, Object> route = new LinkedHashMap<>();
                ResultSetMetaData metadata = resultSet.getMetaData();

                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    route.put(
                            metadata.getColumnLabel(i),
                            resultSet.getObject(i)
                    );
                }

                return route;
            }

        } catch (SQLException e) {
            logger.error("Unable to retrieve route {}", routeId, e);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve route."
            );
        }
    }

    // =====================================================
    // REQUEST AND RESPONSE RECORDS
    // =====================================================

    public record RouteRequest(
            String start,
            String destination
    ) {}

    public record RoadRouteRequest(
            double startLatitude,
            double startLongitude,
            double destinationLatitude,
            double destinationLongitude
    ) {}

    public record DeliveryStop(
            String name,
            double latitude,
            double longitude
    ) {}

    public record MultiStopRouteRequest(
            String startName,
            double startLatitude,
            double startLongitude,
            List<DeliveryStop> stops,
            List<Long> selectedOrderIds
    ) {}

    public record StopResult(
            int sequence,
            String name,
            double latitude,
            double longitude
    ) {}

    public record RouteResponse(
            boolean found,
            String start,
            String destination,
            List<String> route,
            Double totalDistance,
            String message
    ) {}

    public record RoadRouteResponse(
            boolean found,
            double distanceKm,
            double durationMinutes,
            Object geometry,
            String message
    ) {}

    public record MultiStopRouteResponse(
            boolean found,
            Long routeId,
            List<StopResult> optimizedStops,
            double distanceKm,
            double durationMinutes,
            Object geometry,
            String message
    ) {}
}
