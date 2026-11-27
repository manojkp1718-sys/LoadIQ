
package com.loadiq.algorithm;

import com.loadiq.model.Order;
import com.loadiq.model.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KnapsackOptimizer {

    private static final int WEIGHT_SCALE = 1000;
    private static final int VOLUME_SCALE = 1000;

    private static final int MAX_SCALED_CAPACITY = 1000000;
    private static final int MAX_DP_STATES = 250000;

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

        // Convert both capacities into integer units.
        int weightCapacityUnits = toCapacityUnits(
                vehicle.getCapacity(),
                WEIGHT_SCALE,
                "weight"
        );

        int volumeCapacityUnits = toCapacityUnits(
                vehicle.getVolumeCapacity(),
                VOLUME_SCALE,
                "volume"
        );

        List<Order> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        List<Integer> volumes = new ArrayList<>();
        List<Double> scores = new ArrayList<>();

        // Step 1: Prepare valid pending orders.
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

            int weightUnits = toOrderUnits(
                    weight, WEIGHT_SCALE
            );

            int volumeUnits = toOrderUnits(
                    volume, VOLUME_SCALE
            );

            // Skip orders that cannot fit individually.
            if (weightUnits > weightCapacityUnits
                    || volumeUnits > volumeCapacityUnits) {
                continue;
            }

            candidates.add(order);
            weights.add(weightUnits);
            volumes.add(volumeUnits);

            scores.add(
                    priorityScorer.calculateScore(
                            order, referenceTime
                    )
            );
        }

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        /*
         * Sparse two-dimensional 0/1 Knapsack DP.
         *
         * Each state represents:
         * - Weight units already used.
         * - Volume units already used.
         *
         * Only reachable states are stored.
         */
        Map<CapacityState, SelectionNode> states =
                new HashMap<>();

        states.put(
                new CapacityState(0, 0),
                new SelectionNode(-1, 0.0, null)
        );

        // Step 2: Process each order exactly once.
        for (int i = 0; i < candidates.size(); i++) {

            int orderWeight = weights.get(i);
            int orderVolume = volumes.get(i);
            double orderScore = scores.get(i);

            // Snapshot prevents selecting the same order twice.
            List<Map.Entry<CapacityState, SelectionNode>> previousStates =
                    new ArrayList<>(states.entrySet());

            for (Map.Entry<CapacityState, SelectionNode> entry
                    : previousStates) {

                CapacityState previousState = entry.getKey();
                SelectionNode previousNode = entry.getValue();

                int newWeight =
                        previousState.weightUnits() + orderWeight;

                int newVolume =
                        previousState.volumeUnits() + orderVolume;

                // Both constraints must be satisfied.
                if (newWeight > weightCapacityUnits
                        || newVolume > volumeCapacityUnits) {
                    continue;
                }

                double newScore =
                        previousNode.totalScore + orderScore;

                CapacityState newState =
                        new CapacityState(newWeight, newVolume);

                SelectionNode existingNode = states.get(newState);

                // Keep the better score for the same capacity state.
                if (existingNode == null
                        || newScore > existingNode.totalScore + EPSILON) {

                    states.put(
                            newState,
                            new SelectionNode(
                                    i,
                                    newScore,
                                    previousNode
                            )
                    );
                }
            }

            /*
             * Protect against excessive memory consumption.
             * If this limit is reached, fail clearly rather than
             * silently returning an incomplete optimization result.
             */
            if (states.size() > MAX_DP_STATES) {
                throw new IllegalStateException(
                        "Knapsack optimization exceeded "
                        + MAX_DP_STATES
                        + " DP states. Reduce the number of pending "
                        + "orders or use a smaller optimization batch."
                );
            }
        }

        // Step 3: Find the state with the highest total score.
        SelectionNode bestNode = null;
        CapacityState bestState = null;

        for (Map.Entry<CapacityState, SelectionNode> entry
                : states.entrySet()) {

            CapacityState state = entry.getKey();
            SelectionNode node = entry.getValue();

            if (bestNode == null
                    || node.totalScore > bestNode.totalScore + EPSILON
                    || (Math.abs(
                            node.totalScore - bestNode.totalScore
                    ) <= EPSILON
                    && isBetterTie(state, bestState))) {

                bestNode = node;
                bestState = state;
            }
        }

        // Step 4: Reconstruct selected orders.
        List<Order> selectedOrders = new ArrayList<>();

        SelectionNode current = bestNode;

        while (current != null && current.itemIndex >= 0) {

            selectedOrders.add(
                    candidates.get(current.itemIndex)
            );

            current = current.previous;
        }

        Collections.reverse(selectedOrders);

        return selectedOrders;
    }

    private int toCapacityUnits(
            double capacity,
            int scale,
            String capacityName) {

        int units;

        try {
            units = BigDecimal.valueOf(capacity)
                    .multiply(BigDecimal.valueOf(scale))
                    .setScale(0, RoundingMode.FLOOR)
                    .intValueExact();

        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Vehicle " + capacityName
                    + " capacity is outside the supported range."
            );
        }

        if (units <= 0) {
            throw new IllegalArgumentException(
                    "Vehicle " + capacityName
                    + " capacity is too small for the supported precision."
            );
        }

        if (units > MAX_SCALED_CAPACITY) {
            throw new IllegalArgumentException(
                    "Vehicle " + capacityName
                    + " capacity exceeds the supported limit."
            );
        }

        return units;
    }

    private int toOrderUnits(
            double value,
            int scale) {

        try {
            return BigDecimal.valueOf(value)
                    .multiply(BigDecimal.valueOf(scale))
                    .setScale(0, RoundingMode.CEILING)
                    .intValueExact();

        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Order weight or volume is outside the supported range."
            );
        }
    }

    private boolean isBetterTie(
            CapacityState candidate,
            CapacityState currentBest) {

        if (currentBest == null) {
            return true;
        }

        // Prefer less weight when total scores are equal.
        if (candidate.weightUnits() != currentBest.weightUnits()) {
            return candidate.weightUnits() < currentBest.weightUnits();
        }

        // Then prefer less volume.
        return candidate.volumeUnits() < currentBest.volumeUnits();
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
        System.out.println("    KNAPSACK OPTIMIZATION RESULTS");
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

    private record CapacityState(
            int weightUnits,
            int volumeUnits
    ) {}

    private static final class SelectionNode {

        private final int itemIndex;
        private final double totalScore;
        private final SelectionNode previous;

        private SelectionNode(
                int itemIndex,
                double totalScore,
                SelectionNode previous) {

            this.itemIndex = itemIndex;
            this.totalScore = totalScore;
            this.previous = previous;
        }
    }
}
