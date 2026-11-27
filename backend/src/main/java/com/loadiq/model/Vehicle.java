
package com.loadiq.model;

public class Vehicle {

    private int vehicleId;
    private String registrationNumber;
    private String vehicleType;
    private double capacity;
    private double volumeCapacity;
    private String availability;

    // Backward-compatible constructor
    public Vehicle(
            String registrationNumber,
            String vehicleType,
            double capacity) {

        this(
                registrationNumber,
                vehicleType,
                capacity,
                0.0
        );
    }

    // Constructor with volume capacity
    public Vehicle(
            String registrationNumber,
            String vehicleType,
            double capacity,
            double volumeCapacity) {

        this.registrationNumber = registrationNumber;
        this.vehicleType = vehicleType;
        this.capacity = capacity;
        this.volumeCapacity = volumeCapacity;
        this.availability = "AVAILABLE";
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getVolumeCapacity() {
        return volumeCapacity;
    }

    public void setVolumeCapacity(double volumeCapacity) {
        this.volumeCapacity = volumeCapacity;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }
}
