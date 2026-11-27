
package com.loadiq.service;

import com.loadiq.model.Vehicle;
import com.loadiq.repository.VehicleRepository;

import java.util.List;

public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService() {
        this.vehicleRepository = new VehicleRepository();
    }

    // Backward-compatible method
    public boolean addVehicle(
            String registrationNumber,
            String vehicleType,
            double capacity) {

        return addVehicleInternal(
                registrationNumber,
                vehicleType,
                capacity,
                0.0,
                true
        );
    }

    // New method with volume capacity
    public boolean addVehicle(
            String registrationNumber,
            String vehicleType,
            double capacity,
            double volumeCapacity) {

        return addVehicleInternal(
                registrationNumber,
                vehicleType,
                capacity,
                volumeCapacity,
                false
        );
    }

    private boolean addVehicleInternal(
            String registrationNumber,
            String vehicleType,
            double capacity,
            double volumeCapacity,
            boolean allowLegacyZeroVolume) {

        if (registrationNumber == null
                || registrationNumber.isBlank()) {
            return false;
        }

        if (vehicleType == null || vehicleType.isBlank()) {
            return false;
        }

        if (!Double.isFinite(capacity) || capacity <= 0) {
            return false;
        }

        if (!Double.isFinite(volumeCapacity)
                || (allowLegacyZeroVolume
                    ? volumeCapacity < 0
                    : volumeCapacity <= 0)) {
            return false;
        }

        Vehicle vehicle = new Vehicle(
                registrationNumber.trim().toUpperCase(),
                vehicleType.trim(),
                capacity,
                volumeCapacity
        );

        vehicle.setAvailability("AVAILABLE");

        return vehicleRepository.addVehicle(vehicle);
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.getAllVehicles();
    }

    public Vehicle getVehicleById(int vehicleId) {

        if (vehicleId <= 0) {
            return null;
        }

        return vehicleRepository.getVehicleById(vehicleId);
    }

    public boolean updateAvailability(
            int vehicleId,
            String availability) {

        if (vehicleId <= 0 || availability == null
                || availability.isBlank()) {
            return false;
        }

        String status = availability.trim().toUpperCase();

        if (!status.equals("AVAILABLE")
                && !status.equals("MAINTENANCE")) {
            return false;
        }

        return vehicleRepository.updateAvailability(
                vehicleId, status
        );
    }

    public boolean deleteVehicle(int vehicleId) {

        if (vehicleId <= 0) {
            return false;
        }

        return vehicleRepository.deleteVehicle(vehicleId);
    }
}
