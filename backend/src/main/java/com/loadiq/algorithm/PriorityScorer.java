
package com.loadiq.algorithm;

import com.loadiq.model.Order;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

public class PriorityScorer {

    private static final double PRIORITY_WEIGHT = 50.0;
    private static final double DEADLINE_WEIGHT = 30.0;
    private static final double DELIVERY_WEIGHT = 20.0;

    // Supports both yyyy-MM-dd HH:mm and yyyy-MM-dd HH:mm:ss
    private static final DateTimeFormatter DEADLINE_FORMAT =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm")
                    .optionalStart()
                    .appendPattern(":ss")
                    .optionalEnd()
                    .toFormatter();

    public double calculateScore(Order order) {
        return calculateScore(order, LocalDateTime.now());
    }

    public double calculateScore(
            Order order,
            LocalDateTime referenceTime) {

        if (order == null || referenceTime == null) {
            throw new IllegalArgumentException(
                    "Order and reference time cannot be null."
            );
        }

        if (order.getPriority() < 1 || order.getPriority() > 5) {
            throw new IllegalArgumentException(
                    "Priority must be between 1 and 5."
            );
        }

        if (!Double.isFinite(order.getWeight())
                || order.getWeight() <= 0) {
            throw new IllegalArgumentException(
                    "Order weight must be positive."
            );
        }

        return calculatePriorityScore(order)
                + calculateDeadlineScore(order, referenceTime)
                + calculateDeliveryScore(order);
    }

    private double calculatePriorityScore(Order order) {
        return (order.getPriority() / 5.0) * PRIORITY_WEIGHT;
    }

    private double calculateDeadlineScore(
            Order order,
            LocalDateTime referenceTime) {

        String deadline = order.getDeadline();

        if (deadline == null || deadline.isBlank()) {
            return 0.0;
        }

        try {
            LocalDateTime deadlineTime =
                    LocalDateTime.parse(
                            deadline.trim(),
                            DEADLINE_FORMAT
                    );

            long minutesRemaining = Duration.between(
                    referenceTime,
                    deadlineTime
            ).toMinutes();

            if (minutesRemaining <= 0) {
                return 30.0;
            }

            if (minutesRemaining <= 6 * 60) {
                return 25.0;
            }

            if (minutesRemaining <= 12 * 60) {
                return 20.0;
            }

            if (minutesRemaining <= 24 * 60) {
                return 15.0;
            }

            if (minutesRemaining <= 48 * 60) {
                return 10.0;
            }

            return 5.0;

        } catch (DateTimeParseException e) {
            return 0.0;
        }
    }

    private double calculateDeliveryScore(Order order) {

        double weight = order.getWeight();

        if (weight <= 10) {
            return 20.0;
        }

        if (weight <= 50) {
            return 15.0;
        }

        if (weight <= 100) {
            return 10.0;
        }

        if (weight <= 500) {
            return 5.0;
        }

        return 2.0;
    }

    public void displayScore(Order order) {

        double score = calculateScore(order);

        System.out.println("--------------------------------");
        System.out.println("Order ID: " + order.getOrderId());
        System.out.println("Customer: " + order.getCustomerName());
        System.out.println("Priority: " + order.getPriority());
        System.out.println("Weight: " + order.getWeight() + " kg");

        System.out.printf(
                "Priority Score: %.2f/100%n",
                score
        );
    }
}
