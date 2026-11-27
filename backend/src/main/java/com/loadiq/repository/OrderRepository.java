package com.loadiq.repository;

import com.loadiq.config.DatabaseConnection;
import com.loadiq.model.Order;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OrderRepository {

    public boolean addOrder(Order order) {

        if (order == null) {
            return false;
        }

        String sql = """
            INSERT INTO orders
            (customer_name, destination, weight, volume,
             priority, deadline, order_status,
             destination_latitude, destination_longitude)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, order.getCustomerName());
            ps.setString(2, order.getDestination());
            ps.setDouble(3, order.getWeight());
            ps.setDouble(4, order.getVolume());
            ps.setInt(5, order.getPriority());

            if (order.getDeadline() == null
                    || order.getDeadline().isBlank()) {
                ps.setNull(6, Types.TIMESTAMP);
            } else {
                ps.setString(6, order.getDeadline());
            }

            ps.setString(7, order.getOrderStatus());

            setCoordinate(
                    ps, 8, order.getDestinationLatitude()
            );

            setCoordinate(
                    ps, 9, order.getDestinationLongitude()
            );

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        order.setOrderId(rs.getInt(1));
                    }
                }

                System.out.println("Order added successfully!");
                return true;
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error adding order: " + e.getMessage()
            );
        }

        return false;
    }

    public List<Order> getAllOrders() {

        List<Order> orders = new ArrayList<>();

        String sql =
                "SELECT * FROM orders ORDER BY order_id ASC";

        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                orders.add(mapOrder(rs));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error retrieving orders: " + e.getMessage()
            );
        }

        return orders;
    }

    public List<Order> getPendingOrders() {

        List<Order> orders = new ArrayList<>();

        String sql = """
            SELECT * FROM orders
            WHERE order_status = 'PENDING'
            ORDER BY order_id ASC
            """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                orders.add(mapOrder(rs));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error retrieving pending orders: "
                            + e.getMessage()
            );
        }

        return orders;
    }

    public Order getOrderById(int id) {

        if (id <= 0) {
            return null;
        }

        String sql =
                "SELECT * FROM orders WHERE order_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error searching order: " + e.getMessage()
            );
        }

        return null;
    }

    // Retrieve existing orders that need geocoding.
    public List<Order> getOrdersMissingCoordinates() {

        List<Order> orders = new ArrayList<>();

        String sql = """
            SELECT * FROM orders
            WHERE destination_latitude IS NULL
               OR destination_longitude IS NULL
            ORDER BY order_id ASC
            """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                orders.add(mapOrder(rs));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error retrieving orders without coordinates: "
                            + e.getMessage()
            );
        }

        return orders;
    }

    // Update coordinates without modifying other order fields.
    public boolean updateCoordinates(
            int orderId,
            double latitude,
            double longitude) {

        if (orderId <= 0
                || !Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90
                || latitude > 90
                || longitude < -180
                || longitude > 180) {
            return false;
        }

        String sql = """
            UPDATE orders
            SET destination_latitude = ?,
                destination_longitude = ?
            WHERE order_id = ?
            """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDouble(1, latitude);
            ps.setDouble(2, longitude);
            ps.setInt(3, orderId);

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println(
                    "Error updating order coordinates: "
                            + e.getMessage()
            );
            return false;
        }
    }

    public boolean updateOrderStatus(int id, String status) {

        if (id <= 0 || status == null || status.isBlank()) {
            return false;
        }

        String newStatus = status.trim().toUpperCase();

        if (!newStatus.equals("CANCELLED")
                && !newStatus.equals("DELIVERED")) {
            return false;
        }

        String selectOrderSql = """
            SELECT order_status
            FROM orders
            WHERE order_id = ?
            FOR UPDATE
            """;

        String selectAssignmentsSql = """
            SELECT vehicle_id
            FROM load_assignments
            WHERE order_id = ?
              AND assignment_status = 'ASSIGNED'
            FOR UPDATE
            """;

        String updateOrderSql = """
            UPDATE orders
            SET order_status = ?
            WHERE order_id = ?
            """;

        String completeAssignmentsSql = """
            UPDATE load_assignments
            SET assignment_status = 'COMPLETED'
            WHERE order_id = ?
              AND assignment_status = 'ASSIGNED'
            """;

        String releaseVehicleSql = """
            UPDATE vehicles
            SET availability = 'AVAILABLE'
            WHERE vehicle_id = ?
              AND availability = 'IN_USE'
              AND NOT EXISTS (
                  SELECT 1
                  FROM load_assignments
                  WHERE load_assignments.vehicle_id = vehicles.vehicle_id
                    AND load_assignments.assignment_status = 'ASSIGNED'
              )
            """;

        try (Connection con = DatabaseConnection.getConnection()) {

            con.setAutoCommit(false);

            try {
                String currentStatus;

                try (PreparedStatement ps =
                             con.prepareStatement(selectOrderSql)) {

                    ps.setInt(1, id);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Order not found.");
                        }

                        currentStatus = rs.getString("order_status");
                    }
                }

                boolean validTransition =
                        ("PENDING".equalsIgnoreCase(currentStatus)
                                && "CANCELLED".equals(newStatus))
                        ||
                        ("ASSIGNED".equalsIgnoreCase(currentStatus)
                                && "DELIVERED".equals(newStatus));

                if (!validTransition) {
                    throw new SQLException(
                            "Invalid order status transition."
                    );
                }

                Set<Integer> vehicleIds = new LinkedHashSet<>();

                try (PreparedStatement ps =
                             con.prepareStatement(selectAssignmentsSql)) {

                    ps.setInt(1, id);

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            vehicleIds.add(rs.getInt("vehicle_id"));
                        }
                    }
                }

                if ("DELIVERED".equals(newStatus)
                        && vehicleIds.isEmpty()) {
                    throw new SQLException(
                            "No active assignment found for this order."
                    );
                }

                try (PreparedStatement ps =
                             con.prepareStatement(updateOrderSql)) {

                    ps.setString(1, newStatus);
                    ps.setInt(2, id);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Unable to update order status."
                        );
                    }
                }

                if ("DELIVERED".equals(newStatus)) {

                    try (PreparedStatement ps =
                                 con.prepareStatement(
                                         completeAssignmentsSql)) {

                        ps.setInt(1, id);

                        if (ps.executeUpdate() == 0) {
                            throw new SQLException(
                                    "Unable to complete assignment history."
                            );
                        }
                    }

                    try (PreparedStatement ps =
                                 con.prepareStatement(releaseVehicleSql)) {

                        for (int vehicleId : vehicleIds) {
                            ps.setInt(1, vehicleId);
                            ps.addBatch();
                        }

                        ps.executeBatch();
                    }
                }

                con.commit();

                System.out.println(
                        "Order and assignment status updated successfully."
                );

                return true;

            } catch (SQLException e) {

                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                System.out.println(
                        "Order status update rolled back: "
                                + e.getMessage()
                );

                return false;
            }

        } catch (SQLException e) {

            System.out.println(
                    "Database error updating order status: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public boolean deleteOrder(int id) {

        if (id <= 0) {
            return false;
        }

        String sql = """
            DELETE FROM orders
            WHERE order_id = ?
              AND NOT EXISTS (
                    SELECT 1
                    FROM load_assignments
                    WHERE load_assignments.order_id = orders.order_id
              )
            """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            return ps.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println(
                    "Error deleting order: " + e.getMessage()
            );
            return false;
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {

        Order order = new Order(
                rs.getString("customer_name"),
                rs.getString("destination"),
                rs.getDouble("weight"),
                rs.getDouble("volume"),
                rs.getInt("priority"),
                rs.getString("deadline")
        );

        order.setOrderId(rs.getInt("order_id"));
        order.setOrderStatus(rs.getString("order_status"));

        double latitude =
                rs.getDouble("destination_latitude");

        if (!rs.wasNull()) {
            order.setDestinationLatitude(latitude);
        }

        double longitude =
                rs.getDouble("destination_longitude");

        if (!rs.wasNull()) {
            order.setDestinationLongitude(longitude);
        }

        return order;
    }

    private void setCoordinate(
            PreparedStatement ps,
            int index,
            Double coordinate) throws SQLException {

        if (coordinate == null || !Double.isFinite(coordinate)) {
            ps.setNull(index, Types.DECIMAL);
        } else {
            ps.setDouble(index, coordinate);
        }
    }
}