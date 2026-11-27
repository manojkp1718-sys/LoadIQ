
package com.loadiq.api;

import com.loadiq.config.DatabaseConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@RequestMapping("/api/history")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
public class HistoryController {

    private static final Logger logger =
            LoggerFactory.getLogger(HistoryController.class);

    // =====================================================
    // GET ASSIGNMENT HISTORY
    // =====================================================

    @GetMapping("/assignments")
    public List<Map<String, Object>> getAssignmentHistory() {

        String sql = """
                SELECT
                    la.assignment_id,
                    la.order_id,
                    o.customer_name,
                    o.destination,
                    la.vehicle_id,
                    v.registration_number,
                    la.assigned_weight,
                    la.assigned_volume,
                    la.assigned_at,
                    la.assignment_status
                FROM load_assignments la
                JOIN orders o
                    ON la.order_id = o.order_id
                JOIN vehicles v
                    ON la.vehicle_id = v.vehicle_id
                ORDER BY la.assignment_id DESC
                """;

        return executeQuery(sql);
    }

    // =====================================================
    // GET OPTIMIZATION HISTORY
    // =====================================================

    @GetMapping("/optimizations")
    public List<Map<String, Object>> getOptimizationHistory() {

        String sql = """
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
                """;

        return executeQuery(sql);
    }

    // =====================================================
    // COMMON DATABASE QUERY EXECUTOR
    // =====================================================

    private List<Map<String, Object>> executeQuery(String sql) {

        List<Map<String, Object>> records = new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            int columnCount =
                    resultSet.getMetaData().getColumnCount();

            while (resultSet.next()) {

                Map<String, Object> record =
                        new LinkedHashMap<>();

                for (int i = 1; i <= columnCount; i++) {

                    String columnName =
                            resultSet.getMetaData().getColumnLabel(i);

                    record.put(
                            columnName,
                            resultSet.getObject(i)
                    );
                }

                records.add(record);
            }

            return records;

        } catch (SQLException e) {

            logger.error(
                    "Database error while retrieving history",
                    e
            );

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve history. Please try again later."
            );
        }
    }
}
