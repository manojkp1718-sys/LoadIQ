package com.loadiq.model;

public class Order {

    private int orderId;
    private String customerName;
    private String destination;
    private double weight;
    private double volume;
    private int priority;
    private String deadline;
    private String orderStatus;

    // Nullable coordinates: an address may not yet be geocoded.
    private Double destinationLatitude;
    private Double destinationLongitude;

    // Backward-compatible constructor
    public Order(
            String customerName,
            String destination,
            double weight,
            int priority,
            String deadline) {

        this(customerName, destination, weight, 0.0, priority, deadline);
    }

    // Constructor with volume support
    public Order(
            String customerName,
            String destination,
            double weight,
            double volume,
            int priority,
            String deadline) {

        this.customerName = customerName;
        this.destination = destination;
        this.weight = weight;
        this.volume = volume;
        this.priority = priority;
        this.deadline = deadline;
        this.orderStatus = "PENDING";
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(Double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(Double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }
}