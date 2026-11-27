
package com.loadiq.service;

import com.loadiq.algorithm.FifoBaseline;
import com.loadiq.algorithm.GreedyPriorityOptimizer;
import com.loadiq.algorithm.KnapsackOptimizer;
import com.loadiq.algorithm.PriorityScorer;

import com.loadiq.model.Order;
import com.loadiq.model.Vehicle;

import com.loadiq.repository.OrderRepository;
import com.loadiq.repository.VehicleRepository;
import com.loadiq.repository.AssignmentRepository;
import com.loadiq.repository.OptimisationRepository;

import java.time.LocalDateTime;
import java.util.List;

public class OptimisationService {

    private final OrderRepository orderRepository =
            new OrderRepository();

    private final VehicleRepository vehicleRepository =
            new VehicleRepository();

    private final AssignmentRepository assignmentRepository =
            new AssignmentRepository();

    private final OptimisationRepository optimisationRepository =
            new OptimisationRepository();

    private final FifoBaseline fifoOptimizer =
            new FifoBaseline();

    private final GreedyPriorityOptimizer greedyOptimizer =
            new GreedyPriorityOptimizer();

    private final KnapsackOptimizer knapsackOptimizer =
            new KnapsackOptimizer();

    private final PriorityScorer scorer =
            new PriorityScorer();

    public void runFifoOptimization(int vehicleId, boolean save) {

        Vehicle vehicle = getAvailableVehicle(vehicleId);
        if (vehicle == null) return;

        List<Order> orders = orderRepository.getAllOrders();

        long start = System.nanoTime();

        List<Order> selected =
                fifoOptimizer.optimize(orders, vehicle);

        double executionTime =
                (System.nanoTime() - start) / 1_000_000.0;

        fifoOptimizer.displayResults(selected, vehicle);

        if (save) {
            saveOptimization(
                    "FIFO", orders, selected, executionTime, vehicle
            );
        }
    }

    public void runGreedyOptimization(int vehicleId, boolean save) {

        Vehicle vehicle = getAvailableVehicle(vehicleId);
        if (vehicle == null) return;

        List<Order> orders = orderRepository.getAllOrders();
        LocalDateTime referenceTime = LocalDateTime.now();

        long start = System.nanoTime();

        List<Order> selected =
                greedyOptimizer.optimize(
                        orders, vehicle, referenceTime
                );

        double executionTime =
                (System.nanoTime() - start) / 1_000_000.0;

        greedyOptimizer.displayResults(selected, vehicle);

        if (save) {
            saveOptimization(
                    "GREEDY", orders, selected,
                    executionTime, vehicle, referenceTime
            );
        }
    }

    public void runKnapsackOptimization(int vehicleId, boolean save) {

        Vehicle vehicle = getAvailableVehicle(vehicleId);
        if (vehicle == null) return;

        List<Order> orders = orderRepository.getAllOrders();
        LocalDateTime referenceTime = LocalDateTime.now();

        long start = System.nanoTime();

        List<Order> selected =
                knapsackOptimizer.optimize(
                        orders, vehicle, referenceTime
                );

        double executionTime =
                (System.nanoTime() - start) / 1_000_000.0;

        knapsackOptimizer.displayResults(selected, vehicle);

        if (save) {
            saveOptimization(
                    "KNAPSACK", orders, selected,
                    executionTime, vehicle, referenceTime
            );
        }
    }

    public void compareAlgorithms(int vehicleId) {

        Vehicle vehicle = getAvailableVehicle(vehicleId);
        if (vehicle == null) return;

        List<Order> orders = orderRepository.getAllOrders();
        LocalDateTime referenceTime = LocalDateTime.now();

        long fifoStart = System.nanoTime();
        List<Order> fifoResult =
                fifoOptimizer.optimize(orders, vehicle);
        double fifoTime =
                (System.nanoTime() - fifoStart) / 1_000_000.0;

        long greedyStart = System.nanoTime();
        List<Order> greedyResult =
                greedyOptimizer.optimize(
                        orders, vehicle, referenceTime
                );
        double greedyTime =
                (System.nanoTime() - greedyStart) / 1_000_000.0;

        long knapsackStart = System.nanoTime();
        List<Order> knapsackResult =
                knapsackOptimizer.optimize(
                        orders, vehicle, referenceTime
                );
        double knapsackTime =
                (System.nanoTime() - knapsackStart) / 1_000_000.0;

        double fifoScore = calculateScore(
                fifoResult, referenceTime
        );

        double greedyScore = calculateScore(
                greedyResult, referenceTime
        );

        double knapsackScore = calculateScore(
                knapsackResult, referenceTime
        );

        System.out.println("\n================================");
        System.out.println("       ALGORITHM COMPARISON");
        System.out.println("================================");

        displayComparison(
                "FIFO", fifoResult, vehicle,
                fifoScore, fifoTime
        );

        displayComparison(
                "GREEDY", greedyResult, vehicle,
                greedyScore, greedyTime
        );

        displayComparison(
                "KNAPSACK", knapsackResult, vehicle,
                knapsackScore, knapsackTime
        );

        System.out.println("\n========== BEST RESULT ==========");

        double bestScore = Math.max(
                fifoScore,
                Math.max(greedyScore, knapsackScore)
        );

        if (fifoScore == bestScore) {
            System.out.println("FIFO achieved the highest score.");
        }

        if (greedyScore == bestScore) {
            System.out.println("GREEDY achieved the highest score.");
        }

        if (knapsackScore == bestScore) {
            System.out.println("KNAPSACK achieved the highest score.");
        }

        System.out.println("=================================");

        System.out.println("\n1. Save FIFO Result");
        System.out.println("2. Save Greedy Result");
        System.out.println("3. Save Knapsack Result");
        System.out.println("0. Do Not Save");
    }

    private void displayComparison(
            String algorithm,
            List<Order> selected,
            Vehicle vehicle,
            double score,
            double executionTime) {

        double weight = calculateWeight(selected);
        double volume = calculateVolume(selected);

        System.out.println("\n" + algorithm);
        System.out.println("Selected Orders: " + selected.size());

        System.out.println(
                "Total Weight: " + weight + " kg"
        );

        System.out.println(
                "Total Volume: " + volume + " m3"
        );

        System.out.printf(
                "Weight Utilization: %.2f%%%n",
                percentage(weight, vehicle.getCapacity())
        );

        System.out.printf(
                "Volume Utilization: %.2f%%%n",
                percentage(volume, vehicle.getVolumeCapacity())
        );

        System.out.println("Total Score: " + score);
        System.out.println("Execution Time: " + executionTime + " ms");
    }

    public boolean saveSelectedResult(
            int vehicleId,
            String algorithmName) {

        Vehicle vehicle = getAvailableVehicle(vehicleId);
        if (vehicle == null) return false;

        List<Order> orders = orderRepository.getAllOrders();

        LocalDateTime referenceTime = LocalDateTime.now();

        long start = System.nanoTime();

        List<Order> selected;

        if ("FIFO".equalsIgnoreCase(algorithmName)) {

            selected = fifoOptimizer.optimize(orders, vehicle);

        } else if ("GREEDY".equalsIgnoreCase(algorithmName)) {

            selected = greedyOptimizer.optimize(
                    orders, vehicle, referenceTime
            );

        } else if ("KNAPSACK".equalsIgnoreCase(algorithmName)) {

            selected = knapsackOptimizer.optimize(
                    orders, vehicle, referenceTime
            );

        } else {

            System.out.println("Invalid algorithm.");
            return false;
        }

        double executionTime =
                (System.nanoTime() - start) / 1_000_000.0;

        return saveOptimization(
                algorithmName.toUpperCase(),
                orders,
                selected,
                executionTime,
                vehicle,
                referenceTime
        );
    }

    private boolean saveOptimization(
            String algorithm,
            List<Order> allOrders,
            List<Order> selectedOrders,
            double executionTime,
            Vehicle vehicle) {

        return saveOptimization(
                algorithm,
                allOrders,
                selectedOrders,
                executionTime,
                vehicle,
                LocalDateTime.now()
        );
    }

    private boolean saveOptimization(
            String algorithm,
            List<Order> allOrders,
            List<Order> selectedOrders,
            double executionTime,
            Vehicle vehicle,
            LocalDateTime referenceTime) {

        if (selectedOrders == null || selectedOrders.isEmpty()) {
            System.out.println("No orders selected. Nothing to save.");
            return false;
        }

        double totalWeight = calculateWeight(selectedOrders);
        double totalVolume = calculateVolume(selectedOrders);

        if (!Double.isFinite(totalWeight)
                || totalWeight > vehicle.getCapacity() + 1e-7) {

            System.out.println("Vehicle weight capacity exceeded.");
            return false;
        }

        if (!Double.isFinite(totalVolume)
                || totalVolume > vehicle.getVolumeCapacity() + 1e-7) {

            System.out.println("Vehicle volume capacity exceeded.");
            return false;
        }

        for (Order order : selectedOrders) {

            if (!Double.isFinite(order.getWeight())
                    || order.getWeight() <= 0
                    || !Double.isFinite(order.getVolume())
                    || order.getVolume() <= 0) {

                System.out.println(
                        "Selected order has invalid weight or volume."
                );
                return false;
            }
        }

        boolean assigned = assignmentRepository.saveAssignments(
                selectedOrders,
                vehicle.getVehicleId()
        );

        if (!assigned) {
            System.out.println("Unable to save assignments.");
            return false;
        }

        // Save optimization history including total volume.
        boolean historySaved =
                optimisationRepository.saveOptimizationRun(
                        algorithm,
                        allOrders.size(),
                        selectedOrders.size(),
                        totalWeight,
                        totalVolume,
                        calculateScore(selectedOrders, referenceTime),
                        executionTime
                );

        if (historySaved) {
            System.out.println("\nOptimization saved successfully!");
            System.out.println("Orders marked as ASSIGNED.");
            System.out.println("Vehicle marked as IN_USE.");
            System.out.println("Assigned weight: " + totalWeight + " kg");
            System.out.println("Assigned volume: " + totalVolume + " m3");
        } else {
            System.out.println(
                    "Warning: Assignments were saved, "
                            + "but optimization history could not be saved."
            );
        }

        return true;
    }

    private Vehicle getAvailableVehicle(int vehicleId) {

        Vehicle vehicle =
                vehicleRepository.getVehicleById(vehicleId);

        if (vehicle == null) {
            System.out.println("Vehicle not found.");
            return null;
        }

        if (!"AVAILABLE".equalsIgnoreCase(vehicle.getAvailability())) {
            System.out.println("Vehicle is not available.");
            return null;
        }

        if (!Double.isFinite(vehicle.getCapacity())
                || vehicle.getCapacity() <= 0) {
            System.out.println("Invalid vehicle weight capacity.");
            return null;
        }

        if (!Double.isFinite(vehicle.getVolumeCapacity())
                || vehicle.getVolumeCapacity() <= 0) {
            System.out.println("Invalid vehicle volume capacity.");
            return null;
        }

        return vehicle;
    }

    private double calculateWeight(List<Order> orders) {

        return orders.stream()
                .mapToDouble(Order::getWeight)
                .sum();
    }

    private double calculateVolume(List<Order> orders) {

        return orders.stream()
                .mapToDouble(Order::getVolume)
                .sum();
    }

    private double calculateScore(
            List<Order> orders,
            LocalDateTime referenceTime) {

        return orders.stream()
                .mapToDouble(order ->
                        scorer.calculateScore(order, referenceTime)
                )
                .sum();
    }

    private double percentage(double used, double capacity) {

        if (capacity <= 0) {
            return 0.0;
        }

        return (used / capacity) * 100.0;
    }
}
