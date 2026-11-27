
package com.loadiq.repository;

import com.loadiq.config.DatabaseConnection;
import com.loadiq.model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AssignmentRepository {

    public boolean saveAssignments(List<Order> orders, int vehicleId) {

        if (orders == null || orders.isEmpty() || vehicleId <= 0) {
            System.out.println("Invalid assignment details.");
            return false;
        }

        Set<Integer> orderIds = new HashSet<>();

        double requestedWeight = 0.0;
        double requestedVolume = 0.0;

        for (Order order : orders) {

            if (order == null
                    || order.getOrderId() <= 0
                    || !Double.isFinite(order.getWeight())
                    || order.getWeight() <= 0
                    || !Double.isFinite(order.getVolume())
                    || order.getVolume() <= 0
                    || !orderIds.add(order.getOrderId())) {

                System.out.println("Invalid or duplicate order details.");
                return false;
            }

            requestedWeight += order.getWeight();
            requestedVolume += order.getVolume();
        }

        if (!Double.isFinite(requestedWeight)
                || !Double.isFinite(requestedVolume)) {
            return false;
        }

        String vehicleSql = """
            SELECT capacity, volume_capacity, availability
            FROM vehicles
            WHERE vehicle_id = ?
            FOR UPDATE
            """;

        String existingVehicleAssignmentsSql = """
            SELECT assigned_weight, assigned_volume
            FROM load_assignments
            WHERE vehicle_id = ?
              AND assignment_status = 'ASSIGNED'
            FOR UPDATE
            """;

        String orderSql = """
            SELECT order_status
            FROM orders
            WHERE order_id = ?
            FOR UPDATE
            """;

        String existingOrderAssignmentSql = """
            SELECT assignment_id
            FROM load_assignments
            WHERE order_id = ?
              AND assignment_status = 'ASSIGNED'
            FOR UPDATE
            """;

        String updateOrderSql = """
            UPDATE orders
            SET order_status = 'ASSIGNED'
            WHERE order_id = ?
              AND order_status = 'PENDING'
            """;

        String assignmentSql = """
            INSERT INTO load_assignments
            (
                order_id,
                vehicle_id,
                assigned_weight,
                assigned_volume,
                assignment_status
            )
            VALUES (?, ?, ?, ?, 'ASSIGNED')
            """;

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // Step 1: Lock and validate the vehicle.
                double weightCapacity;
                double volumeCapacity;

                try (PreparedStatement statement =
                             connection.prepareStatement(vehicleSql)) {

                    statement.setInt(1, vehicleId);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new SQLException("Vehicle not found.");
                        }

                        weightCapacity = result.getDouble("capacity");
                        volumeCapacity =
                                result.getDouble("volume_capacity");

                        String availability =
                                result.getString("availability");

                        if (!"AVAILABLE".equalsIgnoreCase(availability)) {
                            throw new SQLException(
                                    "Vehicle is not available."
                            );
                        }
                    }
                }

                if (!Double.isFinite(weightCapacity)
                        || weightCapacity <= 0
                        || !Double.isFinite(volumeCapacity)
                        || volumeCapacity <= 0) {
                    throw new SQLException("Invalid vehicle capacities.");
                }

                // Step 2: Check existing active vehicle assignments.
                double existingWeight = 0.0;
                double existingVolume = 0.0;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     existingVehicleAssignmentsSql)) {

                    statement.setInt(1, vehicleId);

                    try (ResultSet result = statement.executeQuery()) {

                        while (result.next()) {
                            existingWeight +=
                                    result.getDouble("assigned_weight");

                            existingVolume +=
                                    result.getDouble("assigned_volume");
                        }
                    }
                }

                if (existingWeight + requestedWeight
                        > weightCapacity + 1e-7) {
                    throw new SQLException(
                            "Vehicle weight capacity would be exceeded."
                    );
                }

                if (existingVolume + requestedVolume
                        > volumeCapacity + 1e-7) {
                    throw new SQLException(
                            "Vehicle volume capacity would be exceeded."
                    );
                }

                // Step 3: Lock orders in a consistent order.
                List<Integer> sortedOrderIds =
                        new ArrayList<>(orderIds);

                sortedOrderIds.sort(Comparator.naturalOrder());

                try (PreparedStatement orderStatement =
                             connection.prepareStatement(orderSql);
                     PreparedStatement activeStatement =
                             connection.prepareStatement(
                                     existingOrderAssignmentSql)) {

                    for (int orderId : sortedOrderIds) {

                        orderStatement.setInt(1, orderId);

                        try (ResultSet result =
                                     orderStatement.executeQuery()) {

                            if (!result.next()) {
                                throw new SQLException(
                                        "Order " + orderId + " not found."
                                );
                            }

                            String status =
                                    result.getString("order_status");

                            if (!"PENDING".equalsIgnoreCase(status)) {
                                throw new SQLException(
                                        "Order " + orderId
                                                + " is not pending."
                                );
                            }
                        }

                        // Explicitly prevent a second active assignment.
                        activeStatement.setInt(1, orderId);

                        try (ResultSet result =
                                     activeStatement.executeQuery()) {

                            if (result.next()) {
                                throw new SQLException(
                                        "Order " + orderId
                                                + " already has an active "
                                                + "assignment."
                                );
                            }
                        }
                    }
                }

                // Step 4: Update order statuses and insert assignments.
                try (PreparedStatement orderStatement =
                             connection.prepareStatement(updateOrderSql);
                     PreparedStatement assignmentStatement =
                             connection.prepareStatement(assignmentSql)) {

                    for (Order order : orders) {

                        orderStatement.setInt(1, order.getOrderId());

                        if (orderStatement.executeUpdate() != 1) {
                            throw new SQLException(
                                    "Unable to update order "
                                            + order.getOrderId()
                            );
                        }

                        assignmentStatement.setInt(
                                1, order.getOrderId()
                        );

                        assignmentStatement.setInt(2, vehicleId);

                        assignmentStatement.setDouble(
                                3, order.getWeight()
                        );

                        assignmentStatement.setDouble(
                                4, order.getVolume()
                        );

                        if (assignmentStatement.executeUpdate() != 1) {
                            throw new SQLException(
                                    "Unable to save assignment for order "
                                            + order.getOrderId()
                            );
                        }
                    }
                }

                // Step 5: Mark the vehicle as in use.
                String updateVehicleSql = """
                    UPDATE vehicles
                    SET availability = 'IN_USE'
                    WHERE vehicle_id = ?
                      AND availability = 'AVAILABLE'
                    """;

                try (PreparedStatement statement =
                             connection.prepareStatement(updateVehicleSql)) {

                    statement.setInt(1, vehicleId);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Unable to reserve vehicle."
                        );
                    }
                }

                // Step 6: Commit all changes together.
                connection.commit();

                System.out.println(
                        "Load assignments saved successfully!"
                );

                System.out.println(
                        "Duplicate active assignments prevented."
                );

                System.out.println(
                        "Weight and volume capacities verified."
                );

                System.out.println(
                        "Vehicle status updated to IN_USE."
                );

                return true;

            } catch (SQLException e) {

                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                System.out.println(
                        "Assignment failed. Changes rolled back: "
                                + e.getMessage()
                );

                return false;
            }

        } catch (SQLException e) {

            System.out.println(
                    "Database error while saving assignments: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public void viewAllAssignments() {

        String sql = """
            SELECT
                a.assignment_id,
                a.order_id,
                o.customer_name,
                o.destination,
                a.vehicle_id,
                v.registration_number,
                a.assigned_weight,
                a.assigned_volume,
                a.assigned_at,
                a.assignment_status
            FROM load_assignments a
            JOIN orders o ON a.order_id = o.order_id
            JOIN vehicles v ON a.vehicle_id = v.vehicle_id
            ORDER BY a.assignment_id ASC
            """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            System.out.println("\n================================");
            System.out.println("       LOAD ASSIGNMENT HISTORY");
            System.out.println("================================");

            boolean found = false;

            while (result.next()) {

                found = true;

                System.out.println("--------------------------------");

                System.out.println(
                        "Assignment ID: "
                                + result.getInt("assignment_id")
                );

                System.out.println(
                        "Order ID: " + result.getInt("order_id")
                );

                System.out.println(
                        "Customer: " + result.getString("customer_name")
                );

                System.out.println(
                        "Destination: " + result.getString("destination")
                );

                System.out.println(
                        "Vehicle ID: " + result.getInt("vehicle_id")
                );

                System.out.println(
                        "Registration: "
                                + result.getString("registration_number")
                );

                System.out.println(
                        "Weight: "
                                + result.getDouble("assigned_weight")
                                + " kg"
                );

                System.out.println(
                        "Volume: "
                                + result.getDouble("assigned_volume")
                                + " m3"
                );

                System.out.println(
                        "Assigned At: "
                                + result.getString("assigned_at")
                );

                System.out.println(
                        "Status: "
                                + result.getString("assignment_status")
                );
            }

            if (!found) {
                System.out.println("No assignments found.");
            }

            System.out.println("================================");

        } catch (SQLException e) {

            System.out.println(
                    "Unable to retrieve assignments: "
                            + e.getMessage()
            );
        }
    }
}
