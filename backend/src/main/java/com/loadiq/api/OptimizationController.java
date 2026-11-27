
package com.loadiq.api;

import com.loadiq.algorithm.FifoBaseline;
import com.loadiq.algorithm.GreedyPriorityOptimizer;
import com.loadiq.algorithm.KnapsackOptimizer;
import com.loadiq.algorithm.PriorityScorer;

import com.loadiq.model.Order;
import com.loadiq.model.Vehicle;

import com.loadiq.repository.AssignmentRepository;
import com.loadiq.repository.OptimisationRepository;

import com.loadiq.service.OrderService;
import com.loadiq.service.VehicleService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/optimization")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
public class OptimizationController {

    private final OrderService orderService = new OrderService();
    private final VehicleService vehicleService = new VehicleService();

    private final AssignmentRepository assignmentRepository =
            new AssignmentRepository();

    private final OptimisationRepository optimisationRepository =
            new OptimisationRepository();

    private final FifoBaseline fifo = new FifoBaseline();

    private final GreedyPriorityOptimizer greedy =
            new GreedyPriorityOptimizer();

    private final KnapsackOptimizer knapsack =
            new KnapsackOptimizer();

    private final PriorityScorer priorityScorer =
            new PriorityScorer();

    // =====================================================
    // RUN INDIVIDUAL OPTIMIZATION
    // =====================================================

    @PostMapping("/{algorithm}")
    public OptimizationResponse runOptimization(
            @PathVariable String algorithm,
            @RequestBody OptimizationRequest request) {

        if (request == null || request.vehicleId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid vehicle ID is required."
            );
        }

        Vehicle vehicle =
                vehicleService.getVehicleById(request.vehicleId());

        validateVehicle(vehicle);

        if (!"AVAILABLE".equalsIgnoreCase(
                vehicle.getAvailability())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected vehicle is not available."
            );
        }

        // Get all pending orders.
        List<Order> pendingOrders = orderService.getPendingOrders();

        if (pendingOrders == null || pendingOrders.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No pending orders available for optimization."
            );
        }

        // If specific order IDs are provided, optimize only those orders.
        List<Order> orders;

        if (request.orderIds() != null
                && !request.orderIds().isEmpty()) {

            List<Integer> requestedIds = request.orderIds();

            if (requestedIds.stream().anyMatch(
                    id -> id == null || id <= 0)
                    || requestedIds.stream().distinct().count()
                    != requestedIds.size()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Order IDs must be valid and unique."
                );
            }

            orders = pendingOrders.stream()
                    .filter(order ->
                            requestedIds.contains(order.getOrderId()))
                    .toList();

            if (orders.size() != requestedIds.size()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "One or more requested orders are not pending "
                                + "or do not exist."
                );
            }

        } else {
            // Preserve existing behavior when no IDs are supplied.
            orders = pendingOrders;
        }

        LocalDateTime referenceTime = LocalDateTime.now();

        long startTime = System.nanoTime();

        List<Order> selectedOrders;

        switch (algorithm.toLowerCase()) {

            case "fifo" -> selectedOrders =
                    fifo.optimize(new ArrayList<>(orders), vehicle);

            case "greedy" -> selectedOrders =
                    greedy.optimize(
                            new ArrayList<>(orders),
                            vehicle,
                            referenceTime
                    );

            case "knapsack" -> selectedOrders =
                    knapsack.optimize(
                            new ArrayList<>(orders),
                            vehicle,
                            referenceTime
                    );

            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid algorithm. Use fifo, greedy or knapsack."
            );
        }

        double executionTimeMs =
                (System.nanoTime() - startTime) / 1_000_000.0;

        validateCapacity(selectedOrders, vehicle);

        double totalWeight = calculateTotalWeight(selectedOrders);
        double totalVolume = calculateTotalVolume(selectedOrders);

        double totalPriorityScore = calculateTotalScore(
                selectedOrders,
                referenceTime
        );

        double remainingWeight = Math.max(
                0.0,
                vehicle.getCapacity() - totalWeight
        );

        double remainingVolume = Math.max(
                0.0,
                vehicle.getVolumeCapacity() - totalVolume
        );

        boolean assignmentsSaved = false;

        if (request.save() && !selectedOrders.isEmpty()) {

            assignmentsSaved = assignmentRepository.saveAssignments(
                    selectedOrders,
                    vehicle.getVehicleId()
            );

            if (!assignmentsSaved) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Unable to save assignments. "
                                + "Check order and vehicle availability."
                );
            }
        }

        // Save optimization history including total volume.
        boolean historySaved =
                optimisationRepository.saveOptimizationRun(
                        algorithm.toUpperCase(),
                        orders.size(),
                        selectedOrders.size(),
                        totalWeight,
                        totalVolume,
                        totalPriorityScore,
                        executionTimeMs
                );

        if (!historySaved) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to save optimization history."
            );
        }

        List<OrderResult> results = selectedOrders.stream()
                .map(order -> new OrderResult(
                        order.getOrderId(),
                        order.getCustomerName(),
                        order.getDestination(),
                        order.getWeight(),
                        order.getVolume(),
                        order.getPriority(),
                        priorityScorer.calculateScore(
                                order,
                                referenceTime
                        )
                ))
                .toList();

        return new OptimizationResponse(
                algorithm.toUpperCase(),
                vehicle.getVehicleId(),
                vehicle.getRegistrationNumber(),
                vehicle.getCapacity(),
                vehicle.getVolumeCapacity(),
                results.size(),
                totalWeight,
                totalVolume,
                totalPriorityScore,
                remainingWeight,
                remainingVolume,
                calculatePercentage(
                        totalWeight, vehicle.getCapacity()
                ),
                calculatePercentage(
                        totalVolume, vehicle.getVolumeCapacity()
                ),
                executionTimeMs,
                assignmentsSaved,
                results
        );
    }

    // =====================================================
    // COMPARE ALL THREE ALGORITHMS
    // =====================================================

    @PostMapping("/compare")
    public ComparisonResponse compareAlgorithms(
            @RequestBody ComparisonRequest request) {

        if (request == null || request.vehicleId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid vehicle ID is required."
            );
        }

        Vehicle vehicle =
                vehicleService.getVehicleById(request.vehicleId());

        validateVehicle(vehicle);

        if (!"AVAILABLE".equalsIgnoreCase(
                vehicle.getAvailability())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected vehicle is not available for comparison."
            );
        }

        // Comparison is a simulation.
        // It does not reserve the vehicle or save assignments.
        List<Order> orders = orderService.getPendingOrders();

        if (orders == null || orders.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No pending orders available for comparison."
            );
        }

        LocalDateTime referenceTime = LocalDateTime.now();

        // Warm-up executions.
        for (int i = 0; i < 3; i++) {

            fifo.optimize(new ArrayList<>(orders), vehicle);

            greedy.optimize(
                    new ArrayList<>(orders),
                    vehicle,
                    referenceTime
            );

            knapsack.optimize(
                    new ArrayList<>(orders),
                    vehicle,
                    referenceTime
            );
        }

        AlgorithmResult fifoResult = benchmark(
                "FIFO",
                orders,
                vehicle,
                referenceTime,
                (input, selectedVehicle, time) ->
                        fifo.optimize(input, selectedVehicle)
        );

        AlgorithmResult greedyResult = benchmark(
                "GREEDY",
                orders,
                vehicle,
                referenceTime,
                greedy::optimize
        );

        AlgorithmResult knapsackResult = benchmark(
                "KNAPSACK",
                orders,
                vehicle,
                referenceTime,
                knapsack::optimize
        );

        return new ComparisonResponse(
                vehicle.getVehicleId(),
                vehicle.getRegistrationNumber(),
                vehicle.getCapacity(),
                vehicle.getVolumeCapacity(),
                orders.size(),
                List.of(
                        fifoResult,
                        greedyResult,
                        knapsackResult
                )
        );
    }

    // =====================================================
    // BENCHMARK HELPER
    // =====================================================

    private AlgorithmResult benchmark(
            String algorithmName,
            List<Order> orders,
            Vehicle vehicle,
            LocalDateTime referenceTime,
            OptimizerFunction optimizer) {

        List<Double> executionTimes = new ArrayList<>();
        List<Order> selectedOrders = new ArrayList<>();

        for (int i = 0; i < 10; i++) {

            long startTime = System.nanoTime();

            selectedOrders = optimizer.run(
                    new ArrayList<>(orders),
                    vehicle,
                    referenceTime
            );

            long endTime = System.nanoTime();

            executionTimes.add(
                    (endTime - startTime) / 1_000_000.0
            );
        }

        executionTimes.sort(Comparator.naturalOrder());

        double medianExecutionTime =
                (executionTimes.get(4)
                        + executionTimes.get(5)) / 2.0;

        validateCapacity(selectedOrders, vehicle);

        double totalWeight = calculateTotalWeight(selectedOrders);
        double totalVolume = calculateTotalVolume(selectedOrders);

        double totalPriorityScore = calculateTotalScore(
                selectedOrders,
                referenceTime
        );

        double remainingWeight = Math.max(
                0.0,
                vehicle.getCapacity() - totalWeight
        );

        double remainingVolume = Math.max(
                0.0,
                vehicle.getVolumeCapacity() - totalVolume
        );

        double weightUtilization =
                calculatePercentage(
                        totalWeight,
                        vehicle.getCapacity()
                );

        double volumeUtilization =
                calculatePercentage(
                        totalVolume,
                        vehicle.getVolumeCapacity()
                );

        double assignmentRate =
                orders.isEmpty()
                        ? 0.0
                        : (selectedOrders.size() * 100.0)
                        / orders.size();

        List<Integer> selectedOrderIds = selectedOrders.stream()
                .map(Order::getOrderId)
                .toList();

        return new AlgorithmResult(
                algorithmName,
                selectedOrders.size(),
                totalWeight,
                totalVolume,
                totalPriorityScore,
                remainingWeight,
                remainingVolume,
                weightUtilization,
                volumeUtilization,
                assignmentRate,
                medianExecutionTime,
                selectedOrderIds
        );
    }

    // =====================================================
    // COMMON VALIDATION METHODS
    // =====================================================

    private void validateVehicle(Vehicle vehicle) {

        if (vehicle == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Vehicle not found."
            );
        }

        if (!Double.isFinite(vehicle.getCapacity())
                || vehicle.getCapacity() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vehicle weight capacity must be positive."
            );
        }

        if (!Double.isFinite(vehicle.getVolumeCapacity())
                || vehicle.getVolumeCapacity() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vehicle volume capacity must be positive."
            );
        }
    }

    private void validateCapacity(
            List<Order> selectedOrders,
            Vehicle vehicle) {

        if (selectedOrders == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Optimizer returned a null result."
            );
        }

        double totalWeight = 0.0;
        double totalVolume = 0.0;

        for (Order order : selectedOrders) {

            if (order == null
                    || !Double.isFinite(order.getWeight())
                    || order.getWeight() <= 0
                    || !Double.isFinite(order.getVolume())
                    || order.getVolume() <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Optimizer returned an order with invalid "
                                + "weight or volume."
                );
            }

            totalWeight += order.getWeight();
            totalVolume += order.getVolume();
        }

        if (!Double.isFinite(totalWeight)
                || totalWeight > vehicle.getCapacity() + 1e-7) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Selected orders exceed vehicle weight capacity."
            );
        }

        if (!Double.isFinite(totalVolume)
                || totalVolume > vehicle.getVolumeCapacity() + 1e-7) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Selected orders exceed vehicle volume capacity."
            );
        }
    }

    // =====================================================
    // COMMON CALCULATION METHODS
    // =====================================================

    private double calculateTotalWeight(List<Order> orders) {

        return orders.stream()
                .mapToDouble(Order::getWeight)
                .sum();
    }

    private double calculateTotalVolume(List<Order> orders) {

        return orders.stream()
                .mapToDouble(Order::getVolume)
                .sum();
    }

    private double calculateTotalScore(
            List<Order> orders,
            LocalDateTime referenceTime) {

        return orders.stream()
                .mapToDouble(order ->
                        priorityScorer.calculateScore(
                                order,
                                referenceTime
                        )
                )
                .sum();
    }

    private double calculatePercentage(
            double used,
            double capacity) {

        if (capacity <= 0) {
            return 0.0;
        }

        return (used / capacity) * 100.0;
    }

    // =====================================================
    // FUNCTIONAL INTERFACE
    // =====================================================

    @FunctionalInterface
    private interface OptimizerFunction {

        List<Order> run(
                List<Order> orders,
                Vehicle vehicle,
                LocalDateTime referenceTime
        );
    }

    // =====================================================
    // REQUEST RECORDS
    // =====================================================

    public record OptimizationRequest(
            int vehicleId,
            boolean save,
            List<Integer> orderIds
    ) {}

    public record ComparisonRequest(
            int vehicleId
    ) {}

    // =====================================================
    // RESPONSE RECORDS
    // =====================================================

    public record OrderResult(
            int orderId,
            String customerName,
            String destination,
            double weight,
            double volume,
            int priority,
            double priorityScore
    ) {}

    public record OptimizationResponse(
            String algorithm,
            int vehicleId,
            String vehicleRegistration,
            double vehicleCapacity,
            double vehicleVolumeCapacity,
            int selectedOrders,
            double totalWeight,
            double totalVolume,
            double totalPriorityScore,
            double remainingCapacity,
            double remainingVolumeCapacity,
            double weightUtilization,
            double volumeUtilization,
            double executionTimeMs,
            boolean assignmentsSaved,
            List<OrderResult> orders
    ) {}

    public record AlgorithmResult(
            String algorithm,
            int selectedOrders,
            double totalWeight,
            double totalVolume,
            double totalPriorityScore,
            double remainingCapacity,
            double remainingVolumeCapacity,
            double weightUtilization,
            double volumeUtilization,
            double assignmentRate,
            double executionTimeMs,
            List<Integer> selectedOrderIds
    ) {}

    public record ComparisonResponse(
            int vehicleId,
            String vehicleRegistration,
            double vehicleCapacity,
            double vehicleVolumeCapacity,
            int totalPendingOrders,
            List<AlgorithmResult> results
    ) {}
}
