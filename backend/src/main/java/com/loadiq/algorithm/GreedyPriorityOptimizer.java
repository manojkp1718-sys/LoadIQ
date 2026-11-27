
package com.loadiq.algorithm;

import com.loadiq.model.Order;
import com.loadiq.model.Vehicle;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GreedyPriorityOptimizer {

    private static final double EPSILON = 1e-9;

    private final PriorityScorer priorityScorer =
            new PriorityScorer();

    public List<Order> optimize(
            List<Order> orders,
            Vehicle vehicle) {

        return optimize(orders, vehicle, LocalDateTime.now());
    }

    public List<Order> optimize(
            List<Order> orders,
            Vehicle vehicle,
            LocalDateTime referenceTime) {

        if (orders == null || vehicle == null
                || referenceTime == null) {
            throw new IllegalArgumentException(
                    "Orders, vehicle and reference time cannot be null."
            );
        }

        if (!Double.isFinite(vehicle.getCapacity())
                || vehicle.getCapacity() <= 0) {
            throw new IllegalArgumentException(
                    "Vehicle weight capacity must be positive."
            );
        }

        if (!Double.isFinite(vehicle.getVolumeCapacity())
                || vehicle.getVolumeCapacity() <= 0) {
            throw new IllegalArgumentException(
                    "Vehicle volume capacity must be positive."
            );
        }

        // Step 1: Prepare valid pending orders and calculate scores.
        List<ScoredOrder> scoredOrders = new ArrayList<>();

        for (Order order : orders) {

            if (order == null) {
                continue;
            }

            if (!"PENDING".equalsIgnoreCase(
                    order.getOrderStatus())) {
                continue;
            }

            double weight = order.getWeight();
            double volume = order.getVolume();

            if (!Double.isFinite(weight) || weight <= 0
                    || !Double.isFinite(volume) || volume <= 0) {
                continue;
            }

            if (weight > vehicle.getCapacity() + EPSILON
                    || volume > vehicle.getVolumeCapacity() + EPSILON) {
                continue;
            }

            double score = priorityScorer.calculateScore(
                    order, referenceTime
            );

            scoredOrders.add(new ScoredOrder(order, score));
        }

        // Step 2: Sort by highest priority score first.
        // Order ID provides deterministic tie-breaking.
        scoredOrders.sort(
                Comparator.comparingDouble(ScoredOrder::score)
                        .reversed()
                        .thenComparingInt(
                                item -> item.order().getOrderId()
                        )
        );

        // Step 3: Select orders within both capacity limits.
        List<Order> selectedOrders = new ArrayList<>();

        double remainingWeight = vehicle.getCapacity();
        double remainingVolume = vehicle.getVolumeCapacity();

        for (ScoredOrder item : scoredOrders) {

            Order order = item.order();

            double weight = order.getWeight();
            double volume = order.getVolume();

            if (weight <= remainingWeight + EPSILON
                    && volume <= remainingVolume + EPSILON) {

                selectedOrders.add(order);

                remainingWeight -= weight;
                remainingVolume -= volume;
            }
        }

        return selectedOrders;
    }

    public void displayResults(
            List<Order> selectedOrders,
            Vehicle vehicle) {

        if (selectedOrders == null || vehicle == null) {
            throw new IllegalArgumentException(
                    "Selected orders and vehicle cannot be null."
            );
        }

        PriorityScorer scorer = new PriorityScorer();

        double totalWeight = 0.0;
        double totalVolume = 0.0;
        double totalScore = 0.0;

        LocalDateTime referenceTime = LocalDateTime.now();

        System.out.println("\n================================");
        System.out.println("    GREEDY OPTIMIZATION RESULTS");
        System.out.println("================================");

        if (selectedOrders.isEmpty()) {
            System.out.println("No orders selected.");
        }

        for (Order order : selectedOrders) {

            double score = scorer.calculateScore(
                    order, referenceTime
            );

            totalWeight += order.getWeight();
            totalVolume += order.getVolume();
            totalScore += score;

            System.out.println("--------------------------------");
            System.out.println("Order ID: " + order.getOrderId());
            System.out.println("Customer: " + order.getCustomerName());
            System.out.println("Destination: " + order.getDestination());
            System.out.println("Weight: " + order.getWeight() + " kg");
            System.out.println("Volume: " + order.getVolume() + " m3");

            System.out.printf(
                    "Priority Score: %.2f%n",
                    score
            );
        }

        System.out.println("\n========== SUMMARY ==========");

        System.out.println(
                "Vehicle: " + vehicle.getRegistrationNumber()
        );

        System.out.println(
                "Weight Capacity: " + vehicle.getCapacity() + " kg"
        );

        System.out.println(
                "Volume Capacity: " + vehicle.getVolumeCapacity() + " m3"
        );

        System.out.println(
                "Selected Orders: " + selectedOrders.size()
        );

        System.out.printf(
                "Total Loaded Weight: %.3f kg%n",
                totalWeight
        );

        System.out.printf(
                "Remaining Weight Capacity: %.3f kg%n",
                Math.max(0, vehicle.getCapacity() - totalWeight)
        );

        System.out.printf(
                "Total Loaded Volume: %.3f m3%n",
                totalVolume
        );

        System.out.printf(
                "Remaining Volume Capacity: %.3f m3%n",
                Math.max(0, vehicle.getVolumeCapacity() - totalVolume)
        );

        System.out.printf(
                "Total Priority Score: %.2f%n",
                totalScore
        );

        System.out.println("=============================");
    }

    private record ScoredOrder(
            Order order,
            double score
    ) {}
}
