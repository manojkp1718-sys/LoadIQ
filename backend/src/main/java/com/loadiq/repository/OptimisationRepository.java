
package com.loadiq.repository;

import com.loadiq.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OptimisationRepository {

    // Backward-compatible method for existing callers.
    // Existing calls will save totalVolume as 0.
    public boolean saveOptimizationRun(
            String algorithmName,
            int totalOrders,
            int assignedOrders,
            double totalWeight,
            double totalPriorityScore,
            double executionTimeMs) {

        return saveOptimizationRun(
                algorithmName,
                totalOrders,
                assignedOrders,
                totalWeight,
                0.0,
                totalPriorityScore,
                executionTimeMs
        );
    }

    // New method supporting both weight and volume.
    public boolean saveOptimizationRun(
            String algorithmName,
            int totalOrders,
            int assignedOrders,
            double totalWeight,
            double totalVolume,
            double totalPriorityScore,
            double executionTimeMs) {

        if (algorithmName == null || algorithmName.isBlank()
                || totalOrders < 0
                || assignedOrders < 0
                || assignedOrders > totalOrders
                || !Double.isFinite(totalWeight)
                || totalWeight < 0
                || !Double.isFinite(totalVolume)
                || totalVolume < 0
                || !Double.isFinite(totalPriorityScore)
                || totalPriorityScore < 0
                || !Double.isFinite(executionTimeMs)
                || executionTimeMs < 0) {

            System.out.println("Invalid optimization history details.");
            return false;
        }

        String sql = """
            INSERT INTO optimisation_runs
            (
                algorithm_name,
                total_orders,
                assigned_orders,
                total_weight,
                total_volume,
                total_priority_score,
                execution_time_ms
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, algorithmName.trim().toUpperCase());
            statement.setInt(2, totalOrders);
            statement.setInt(3, assignedOrders);
            statement.setDouble(4, totalWeight);
            statement.setDouble(5, totalVolume);
            statement.setDouble(6, totalPriorityScore);
            statement.setDouble(7, executionTimeMs);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println(
                    "Error saving optimization: " + e.getMessage()
            );
            return false;
        }
    }

    public void viewOptimizationHistory() {

        String sql = """
            SELECT *
            FROM optimisation_runs
            ORDER BY run_id DESC
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            System.out.println("\n===== OPTIMIZATION HISTORY =====");

            boolean found = false;

            while (result.next()) {

                found = true;

                System.out.println("------------------------------");

                System.out.println(
                        "Run ID: " + result.getInt("run_id")
                );

                System.out.println(
                        "Algorithm: " + result.getString("algorithm_name")
                );

                System.out.println(
                        "Total Orders: " + result.getInt("total_orders")
                );

                System.out.println(
                        "Assigned Orders: " + result.getInt("assigned_orders")
                );

                System.out.println(
                        "Total Weight: "
                                + result.getDouble("total_weight") + " kg"
                );

                System.out.println(
                        "Total Volume: "
                                + result.getDouble("total_volume") + " m³"
                );

                System.out.println(
                        "Priority Score: "
                                + result.getDouble("total_priority_score")
                );

                System.out.println(
                        "Execution Time: "
                                + result.getDouble("execution_time_ms") + " ms"
                );

                System.out.println(
                        "Date: " + result.getString("run_date")
                );
            }

            if (!found) {
                System.out.println("No optimization history found.");
            }

            System.out.println("================================");

        } catch (SQLException e) {
            System.out.println(
                    "Error retrieving history: " + e.getMessage()
            );
        }
    }
}
