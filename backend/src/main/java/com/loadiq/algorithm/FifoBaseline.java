
package com.loadiq.algorithm;

import com.loadiq.model.Order;
import com.loadiq.model.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class FifoBaseline {

    private static final double EPSILON = 1e-9;

    public List<Order> optimize(
            List<Order> orders,
            Vehicle vehicle) {

        if (orders == null || vehicle == null) {
            throw new IllegalArgumentException(
                    "Orders and vehicle cannot be null."
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

        List<Order> selectedOrders = new ArrayList<>();

        double remainingWeight = vehicle.getCapacity();
        double remainingVolume = vehicle.getVolumeCapacity();

        // Process orders in FIFO order.
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

            if (!Double.isFinite(weight)
                    || weight <= 0
                    || !Double.isFinite(volume)
                    || volume <= 0) {
                continue;
            }

            // Skip orders exceeding individual vehicle limits.
            if (weight > vehicle.getCapacity() + EPSILON
                    || volume > vehicle.getVolumeCapacity() + EPSILON) {
                continue;
            }

            // Check both remaining capacities.
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

        double totalWeight = selectedOrders.stream()
                .mapToDouble(Order::getWeight)
                .sum();

        double totalVolume = selectedOrders.stream()
                .mapToDouble(Order::getVolume)
                .sum();

        System.out.println("\n================================");
        System.out.println("       FIFO BASELINE RESULTS");
        System.out.println("================================");

        if (selectedOrders.isEmpty()) {
            System.out.println("No orders selected.");
        }

        for (Order order : selectedOrders) {

            System.out.println("--------------------------------");
            System.out.println("Order ID: " + order.getOrderId());
            System.out.println("Customer: " + order.getCustomerName());
            System.out.println("Destination: " + order.getDestination());
            System.out.println("Weight: " + order.getWeight() + " kg");
            System.out.println("Volume: " + order.getVolume() + " m3");
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

        System.out.println(
                "Total Loaded Weight: " + totalWeight + " kg"
        );

        System.out.println(
                "Remaining Weight Capacity: "
                + Math.max(0, vehicle.getCapacity() - totalWeight)
                + " kg"
        );

        System.out.println(
                "Total Loaded Volume: " + totalVolume + " m3"
        );

        System.out.println(
                "Remaining Volume Capacity: "
                + Math.max(0, vehicle.getVolumeCapacity() - totalVolume)
                + " m3"
        );

        System.out.println("=============================");
    }
}
