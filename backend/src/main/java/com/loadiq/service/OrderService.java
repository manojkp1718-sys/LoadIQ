package com.loadiq.service;

import com.loadiq.model.Order;
import com.loadiq.repository.OrderRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;

public class OrderService {

    private final OrderRepository orderRepository;
    private final GeocodingService geocodingService;

    private static final DateTimeFormatter INPUT_DEADLINE_FORMAT =
            new DateTimeFormatterBuilder()
                    .appendPattern("uuuu-MM-dd HH:mm")
                    .optionalStart()
                    .appendPattern(":ss")
                    .optionalEnd()
                    .toFormatter();

    private static final DateTimeFormatter OUTPUT_DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    public OrderService() {
        this.orderRepository = new OrderRepository();
        this.geocodingService = new GeocodingService();
    }

    // Backward-compatible method
    public boolean addOrder(
            String customerName,
            String destination,
            double weight,
            int priority,
            String deadline) {

        return addOrderInternal(
                customerName,
                destination,
                weight,
                0.0,
                priority,
                deadline,
                true
        );
    }

    // Method with volume support
    public boolean addOrder(
            String customerName,
            String destination,
            double weight,
            double volume,
            int priority,
            String deadline) {

        return addOrderInternal(
                customerName,
                destination,
                weight,
                volume,
                priority,
                deadline,
                false
        );
    }

    private boolean addOrderInternal(
            String customerName,
            String destination,
            double weight,
            double volume,
            int priority,
            String deadline,
            boolean allowLegacyZeroVolume) {

        if (customerName == null || customerName.isBlank()) {
            System.out.println("Customer name cannot be empty.");
            return false;
        }

        if (destination == null || destination.isBlank()) {
            System.out.println("Destination cannot be empty.");
            return false;
        }

        if (!Double.isFinite(weight) || weight <= 0) {
            System.out.println(
                    "Weight must be positive and finite."
            );
            return false;
        }

        if (!Double.isFinite(volume)
                || (allowLegacyZeroVolume
                ? volume < 0
                : volume <= 0)) {

            System.out.println(
                    "Volume must be positive and finite."
            );
            return false;
        }

        if (priority < 1 || priority > 5) {
            System.out.println(
                    "Priority must be between 1 and 5."
            );
            return false;
        }

        String normalizedDeadline = normalizeDeadline(deadline);

        if (normalizedDeadline == null) {
            System.out.println(
                    "Invalid deadline. Use yyyy-MM-dd HH:mm "
                            + "or yyyy-MM-dd HH:mm:ss."
            );
            return false;
        }

        Order order = new Order(
                customerName.trim(),
                destination.trim(),
                weight,
                volume,
                priority,
                normalizedDeadline
        );

        order.setOrderStatus("PENDING");

        // Attempt geocoding before saving.
        // Failure does not prevent order creation.
        GeocodingService.Coordinate coordinate =
                geocodingService.geocode(destination.trim());

        if (coordinate != null) {
            order.setDestinationLatitude(
                    coordinate.latitude()
            );

            order.setDestinationLongitude(
                    coordinate.longitude()
            );

            System.out.println(
                    "Destination coordinates retrieved successfully."
            );
        } else {
            System.out.println(
                    "Destination coordinates unavailable. "
                            + "Order will be saved without coordinates."
            );
        }

        return orderRepository.addOrder(order);
    }

    private String normalizeDeadline(String deadline) {

        if (deadline == null || deadline.isBlank()) {
            return null;
        }

        try {
            String normalized =
                    deadline.trim().replace('T', ' ');

            LocalDateTime dateTime = LocalDateTime.parse(
                    normalized,
                    INPUT_DEADLINE_FORMAT
            );

            return dateTime.format(OUTPUT_DEADLINE_FORMAT);

        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.getAllOrders();
    }

    public List<Order> getPendingOrders() {
        return orderRepository.getPendingOrders();
    }

    public Order getOrderById(int orderId) {

        if (orderId <= 0) {
            System.out.println("Invalid order ID.");
            return null;
        }

        return orderRepository.getOrderById(orderId);
    }

    /**
     * Geocode existing orders that do not have coordinates.
     * Each successful result is saved immediately.
     */
    public GeocodingSummary geocodeMissingCoordinates() {

        List<Order> orders =
                orderRepository.getOrdersMissingCoordinates();

        int processed = 0;
        int successful = 0;
        int failed = 0;

        for (Order order : orders) {

            processed++;

            GeocodingService.Coordinate coordinate =
                    geocodingService.geocode(
                            order.getDestination()
                    );

            if (coordinate == null) {
                failed++;

                System.out.println(
                        "Could not geocode order "
                                + order.getOrderId()
                                + ": " + order.getDestination()
                );

                continue;
            }

            boolean saved = orderRepository.updateCoordinates(
                    order.getOrderId(),
                    coordinate.latitude(),
                    coordinate.longitude()
            );

            if (saved) {
                successful++;

                System.out.println(
                        "Coordinates saved for order "
                                + order.getOrderId()
                );
            } else {
                failed++;

                System.out.println(
                        "Could not save coordinates for order "
                                + order.getOrderId()
                );
            }
        }

        return new GeocodingSummary(
                orders.size(),
                processed,
                successful,
                failed
        );
    }

    public record GeocodingSummary(
            int totalMissing,
            int processed,
            int successful,
            int failed
    ) {}

    public boolean updateOrderStatus(int orderId, String status) {

        if (orderId <= 0 || status == null || status.isBlank()) {
            System.out.println("Invalid order ID or status.");
            return false;
        }

        String newStatus = status.trim().toUpperCase();

        Order existingOrder =
                orderRepository.getOrderById(orderId);

        if (existingOrder == null) {
            System.out.println("Order not found.");
            return false;
        }

        String currentStatus = existingOrder.getOrderStatus();

        if (currentStatus == null) {
            return false;
        }

        currentStatus = currentStatus.trim().toUpperCase();

        boolean validTransition =
                (currentStatus.equals("PENDING")
                        && newStatus.equals("CANCELLED"))
                ||
                (currentStatus.equals("ASSIGNED")
                        && newStatus.equals("DELIVERED"));

        if (!validTransition) {
            System.out.println(
                    "Invalid status transition: "
                            + currentStatus + " to " + newStatus
            );

            return false;
        }

        return orderRepository.updateOrderStatus(
                orderId,
                newStatus
        );
    }

    public boolean deleteOrder(int orderId) {

        if (orderId <= 0) {
            System.out.println("Invalid order ID.");
            return false;
        }

        return orderRepository.deleteOrder(orderId);
    }
}