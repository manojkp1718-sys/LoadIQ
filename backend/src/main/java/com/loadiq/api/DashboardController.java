
package com.loadiq.api;

import com.loadiq.config.DatabaseConnection;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
public class DashboardController {

    @GetMapping
    public Map<String, Object> getDashboardData() {

        Map<String, Object> dashboard = new LinkedHashMap<>();

        try (Connection connection = DatabaseConnection.getConnection()) {

            // ==========================================
            // ORDER STATISTICS
            // ==========================================

            String orderSql = """
                    SELECT
                        COUNT(*) AS totalOrders,
                        COALESCE(SUM(CASE WHEN order_status = 'PENDING'
                            THEN 1 ELSE 0 END), 0) AS pendingOrders,
                        COALESCE(SUM(CASE WHEN order_status = 'ASSIGNED'
                            THEN 1 ELSE 0 END), 0) AS assignedOrders,
                        COALESCE(SUM(CASE WHEN order_status = 'DELIVERED'
                            THEN 1 ELSE 0 END), 0) AS deliveredOrders,
                        COALESCE(SUM(CASE WHEN order_status = 'CANCELLED'
                            THEN 1 ELSE 0 END), 0) AS cancelledOrders
                    FROM orders
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(orderSql);
                 ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    dashboard.put("totalOrders", rs.getInt("totalOrders"));
                    dashboard.put("pendingOrders", rs.getInt("pendingOrders"));
                    dashboard.put("assignedOrders", rs.getInt("assignedOrders"));
                    dashboard.put("deliveredOrders", rs.getInt("deliveredOrders"));
                    dashboard.put("cancelledOrders", rs.getInt("cancelledOrders"));
                }
            }

            // ==========================================
            // VEHICLE STATISTICS
            // ==========================================

            String vehicleSql = """
                    SELECT
                        COUNT(*) AS totalVehicles,
                        COALESCE(SUM(CASE WHEN availability = 'AVAILABLE'
                            THEN 1 ELSE 0 END), 0) AS availableVehicles,
                        COALESCE(SUM(CASE WHEN availability = 'IN_USE'
                            THEN 1 ELSE 0 END), 0) AS vehiclesInUse,
                        COALESCE(SUM(CASE WHEN availability = 'MAINTENANCE'
                            THEN 1 ELSE 0 END), 0) AS vehiclesInMaintenance
                    FROM vehicles
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(vehicleSql);
                 ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    dashboard.put("totalVehicles", rs.getInt("totalVehicles"));
                    dashboard.put("availableVehicles", rs.getInt("availableVehicles"));
                    dashboard.put("vehiclesInUse", rs.getInt("vehiclesInUse"));
                    dashboard.put(
                            "vehiclesInMaintenance",
                            rs.getInt("vehiclesInMaintenance")
                    );
                }
            }

            // ==========================================
            // RECENT OPTIMIZATION STATISTICS
            // ==========================================

            String historySql = """
                    SELECT
                        run_id,
                        algorithm_name,
                        total_orders,
                        assigned_orders,
                        total_weight,
                        total_volume,
                        total_priority_score,
                        execution_time_ms,
                        run_date
                    FROM optimisation_runs
                    ORDER BY run_id DESC
                    LIMIT 5
                    """;

            List<Map<String, Object>> recentRuns = new ArrayList<>();

            try (PreparedStatement statement =
                         connection.prepareStatement(historySql);
                 ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    Map<String, Object> run = new LinkedHashMap<>();

                    run.put("runId", rs.getInt("run_id"));
                    run.put("algorithmName", rs.getString("algorithm_name"));
                    run.put("totalOrders", rs.getInt("total_orders"));
                    run.put("assignedOrders", rs.getInt("assigned_orders"));
                    run.put("totalWeight", rs.getDouble("total_weight"));
                    run.put("totalVolume", rs.getDouble("total_volume"));
                    run.put(
                            "totalPriorityScore",
                            rs.getDouble("total_priority_score")
                    );
                    run.put(
                            "executionTimeMs",
                            rs.getDouble("execution_time_ms")
                    );
                    run.put("runDate", rs.getString("run_date"));

                    recentRuns.add(run);
                }
            }

            dashboard.put("recentOptimizations", recentRuns);

            // ==========================================
            // ROUTE ANALYTICS
            // ==========================================

            String routeStatsSql = """
                    SELECT
                        COUNT(*) AS totalRoutes,
                        COALESCE(SUM(total_distance_km), 0)
                            AS totalDistanceKm,
                        COALESCE(AVG(total_distance_km), 0)
                            AS averageDistanceKm,
                        COALESCE(AVG(estimated_duration_minutes), 0)
                            AS averageDurationMinutes
                    FROM route_history
                    """;

            try (PreparedStatement statement =
                         connection.prepareStatement(routeStatsSql);
                 ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    dashboard.put("totalRoutes", rs.getInt("totalRoutes"));
                    dashboard.put(
                            "totalDistanceKm",
                            rs.getDouble("totalDistanceKm")
                    );
                    dashboard.put(
                            "averageDistanceKm",
                            rs.getDouble("averageDistanceKm")
                    );
                    dashboard.put(
                            "averageDurationMinutes",
                            rs.getDouble("averageDurationMinutes")
                    );
                }
            }

            // ==========================================
            // RECENT ROUTES
            // ==========================================

            String recentRoutesSql = """
                    SELECT
                        route_id,
                        start_location,
                        total_distance_km,
                        estimated_duration_minutes,
                        created_at
                    FROM route_history
                    ORDER BY route_id DESC
                    LIMIT 5
                    """;

            List<Map<String, Object>> recentRoutes = new ArrayList<>();

            try (PreparedStatement statement =
                         connection.prepareStatement(recentRoutesSql);
                 ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    Map<String, Object> route = new LinkedHashMap<>();

                    route.put("routeId", rs.getLong("route_id"));
                    route.put(
                            "startLocation",
                            rs.getString("start_location")
                    );
                    route.put(
                            "distanceKm",
                            rs.getDouble("total_distance_km")
                    );
                    route.put(
                            "durationMinutes",
                            rs.getDouble("estimated_duration_minutes")
                    );
                    route.put(
                            "createdAt",
                            rs.getString("created_at")
                    );

                    recentRoutes.add(route);
                }
            }

            dashboard.put("recentRoutes", recentRoutes);

            return dashboard;

        } catch (SQLException e) {

            System.out.println(
                    "Dashboard database error: " + e.getMessage()
            );

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load dashboard data: " + e.getMessage()
            );
        }
    }
}
