
package com.loadiq.model;

import java.time.LocalDateTime;

public class LoadAssignment {

    private int assignmentId;
    private int orderId;
    private int vehicleId;
    private double assignedWeight;
    private double assignedVolume;
    private LocalDateTime assignedAt;
    private String assignmentStatus;

    public LoadAssignment() {
    }

    public LoadAssignment(
            int assignmentId,
            int orderId,
            int vehicleId,
            double assignedWeight,
            double assignedVolume,
            LocalDateTime assignedAt,
            String assignmentStatus) {

        this.assignmentId = assignmentId;
        this.orderId = orderId;
        this.vehicleId = vehicleId;
        this.assignedWeight = assignedWeight;
        this.assignedVolume = assignedVolume;
        this.assignedAt = assignedAt;
        this.assignmentStatus = assignmentStatus;
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public double getAssignedWeight() {
        return assignedWeight;
    }

    public void setAssignedWeight(double assignedWeight) {
        this.assignedWeight = assignedWeight;
    }

    public double getAssignedVolume() {
        return assignedVolume;
    }

    public void setAssignedVolume(double assignedVolume) {
        this.assignedVolume = assignedVolume;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }
}
