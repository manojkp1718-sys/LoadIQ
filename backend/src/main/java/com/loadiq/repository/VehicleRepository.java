
package com.loadiq.repository;

import com.loadiq.config.DatabaseConnection;
import com.loadiq.model.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleRepository {

    public boolean addVehicle(Vehicle vehicle) {

        if (vehicle == null) {
            return false;
        }

        String sql = """
            INSERT INTO vehicles
            (registration_number, vehicle_type, capacity,
             volume_capacity, availability)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, vehicle.getRegistrationNumber());
            statement.setString(2, vehicle.getVehicleType());
            statement.setDouble(3, vehicle.getCapacity());
            statement.setDouble(4, vehicle.getVolumeCapacity());
            statement.setString(5, "AVAILABLE");

            int rows = statement.executeUpdate();

            if (rows > 0) {
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        vehicle.setVehicleId(keys.getInt(1));
                    }
                }

                System.out.println("Vehicle added successfully!");
                return true;
            }

        } catch (SQLException e) {
            System.out.println("Unable to add vehicle: " + e.getMessage());
        }

        return false;
    }

    public List<Vehicle> getAllVehicles() {

        List<Vehicle> vehicles = new ArrayList<>();

        String sql =
                "SELECT * FROM vehicles ORDER BY vehicle_id ASC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                vehicles.add(mapVehicle(result));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Unable to retrieve vehicles: " + e.getMessage()
            );
        }

        return vehicles;
    }

    public Vehicle getVehicleById(int vehicleId) {

        if (vehicleId <= 0) {
            return null;
        }

        String sql =
                "SELECT * FROM vehicles WHERE vehicle_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, vehicleId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapVehicle(result);
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Unable to find vehicle: " + e.getMessage()
            );
        }

        return null;
    }

    public boolean updateAvailability(
            int vehicleId,
            String availability) {

        if (vehicleId <= 0 || availability == null
                || availability.isBlank()) {
            return false;
        }

        String status = availability.trim().toUpperCase();

        if ("IN_USE".equals(status)) {
            return false;
        }

        if (!"AVAILABLE".equals(status)
                && !"MAINTENANCE".equals(status)) {
            return false;
        }

        String sql;

        if ("MAINTENANCE".equals(status)) {

            sql = """
                UPDATE vehicles
                SET availability = 'MAINTENANCE'
                WHERE vehicle_id = ?
                  AND availability = 'AVAILABLE'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM load_assignments
                      WHERE load_assignments.vehicle_id = vehicles.vehicle_id
                        AND load_assignments.assignment_status = 'ASSIGNED'
                  )
                """;

        } else {

            sql = """
                UPDATE vehicles
                SET availability = 'AVAILABLE'
                WHERE vehicle_id = ?
                  AND availability = 'MAINTENANCE'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM load_assignments
                      WHERE load_assignments.vehicle_id = vehicles.vehicle_id
                        AND load_assignments.assignment_status = 'ASSIGNED'
                  )
                """;
        }

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, vehicleId);

            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println(
                    "Unable to update vehicle: " + e.getMessage()
            );
            return false;
        }
    }

    public boolean deleteVehicle(int vehicleId) {

        if (vehicleId <= 0) {
            return false;
        }

        String sql = """
            DELETE FROM vehicles
            WHERE vehicle_id = ?
              AND availability <> 'IN_USE'
              AND NOT EXISTS (
                  SELECT 1
                  FROM load_assignments
                  WHERE load_assignments.vehicle_id = vehicles.vehicle_id
              )
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, vehicleId);

            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            System.out.println(
                    "Unable to delete vehicle: " + e.getMessage()
            );
            return false;
        }
    }

    private Vehicle mapVehicle(ResultSet result) throws SQLException {

        Vehicle vehicle = new Vehicle(
                result.getString("registration_number"),
                result.getString("vehicle_type"),
                result.getDouble("capacity"),
                result.getDouble("volume_capacity")
        );

        vehicle.setVehicleId(result.getInt("vehicle_id"));
        vehicle.setAvailability(result.getString("availability"));

        return vehicle;
    }
}
