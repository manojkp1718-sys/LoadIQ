
package com.loadiq.console;

import com.loadiq.model.Order;
import com.loadiq.model.Vehicle;

import com.loadiq.service.OrderService;
import com.loadiq.service.VehicleService;
import com.loadiq.service.OptimisationService;

import com.loadiq.algorithm.RouteOptimizer;

import com.loadiq.repository.AssignmentRepository;
import com.loadiq.repository.OptimisationRepository;

import java.util.List;
import java.util.Scanner;

public class LoadIQMenu {

    private final Scanner scanner = new Scanner(System.in);

    private final OrderService orderService = new OrderService();
    private final VehicleService vehicleService = new VehicleService();

    private final OptimisationService optimisationService =
            new OptimisationService();

    private final RouteOptimizer routeOptimizer = new RouteOptimizer();

    private final AssignmentRepository assignmentRepository =
            new AssignmentRepository();

    private final OptimisationRepository optimisationRepository =
            new OptimisationRepository();

    public LoadIQMenu() {
        routeOptimizer.loadSampleRoutes();
    }

    public void start() {

        int choice;

        do {
            System.out.println("\n================================");
            System.out.println("          LOADIQ SYSTEM");
            System.out.println("================================");

            System.out.println("1. Add New Order");
            System.out.println("2. View All Orders");
            System.out.println("3. Search Order");
            System.out.println("4. Update Order Status");
            System.out.println("5. Delete Order");

            System.out.println("--------------------------------");

            System.out.println("6. Add New Vehicle");
            System.out.println("7. View All Vehicles");
            System.out.println("8. Search Vehicle");
            System.out.println("9. Update Vehicle Availability");
            System.out.println("10. Delete Vehicle");

            System.out.println("--------------------------------");

            System.out.println("11. Run FIFO Optimization");
            System.out.println("12. Run Greedy Optimization");
            System.out.println("13. Run Knapsack Optimization");
            System.out.println("14. Compare All Three Algorithms");
            System.out.println("15. Find Shortest Delivery Route");

            System.out.println("--------------------------------");

            System.out.println("16. View Load Assignment History");
            System.out.println("17. View Optimization History");

            System.out.println("--------------------------------");
            System.out.println("0. Exit");
            System.out.println("================================");

            choice = readInt("Enter your choice: ");

            switch (choice) {

                case 1 -> addOrder();
                case 2 -> viewAllOrders();
                case 3 -> searchOrder();
                case 4 -> updateOrderStatus();
                case 5 -> deleteOrder();

                case 6 -> addVehicle();
                case 7 -> viewAllVehicles();
                case 8 -> searchVehicle();
                case 9 -> updateVehicleAvailability();
                case 10 -> deleteVehicle();

                case 11 -> runFifoOptimization();
                case 12 -> runGreedyOptimization();
                case 13 -> runKnapsackOptimization();
                case 14 -> compareAlgorithms();
                case 15 -> findShortestRoute();

                case 16 -> assignmentRepository.viewAllAssignments();
                case 17 -> optimisationRepository.viewOptimizationHistory();

                case 0 -> System.out.println("Exiting LoadIQ. Goodbye!");

                default -> System.out.println("Invalid choice. Try again.");
            }

        } while (choice != 0);
    }

    // ORDER MANAGEMENT

    private void addOrder() {

        System.out.println("\n--- Add New Order ---");

        String customer = readText("Customer name: ");
        String destination = readText("Destination: ");
        double weight = readDouble("Weight (kg): ");
        int priority = readInt("Priority (1-5): ");
        String deadline = readText("Deadline (yyyy-MM-dd HH:mm): ");

        orderService.addOrder(
            customer, destination, weight, priority, deadline
        );
    }

    private void viewAllOrders() {

        System.out.println("\n--- All Orders ---");

        List<Order> orders = orderService.getAllOrders();

        if (orders.isEmpty()) {
            System.out.println("No orders found.");
            return;
        }

        for (Order order : orders) {

            System.out.println("--------------------------------");
            System.out.println("Order ID: " + order.getOrderId());
            System.out.println("Customer: " + order.getCustomerName());
            System.out.println("Destination: " + order.getDestination());
            System.out.println("Weight: " + order.getWeight() + " kg");
            System.out.println("Priority: " + order.getPriority());
            System.out.println("Deadline: " + order.getDeadline());
            System.out.println("Status: " + order.getOrderStatus());
        }
    }

    private void searchOrder() {

        int id = readInt("Enter Order ID: ");

        Order order = orderService.getOrderById(id);

        if (order == null) {
            System.out.println("Order not found.");
            return;
        }

        System.out.println("Order ID: " + order.getOrderId());
        System.out.println("Customer: " + order.getCustomerName());
        System.out.println("Destination: " + order.getDestination());
        System.out.println("Weight: " + order.getWeight() + " kg");
        System.out.println("Priority: " + order.getPriority());
        System.out.println("Deadline: " + order.getDeadline());
        System.out.println("Status: " + order.getOrderStatus());
    }

    private void updateOrderStatus() {

        int id = readInt("Enter Order ID: ");

        String status = readText(
            "Enter status (PENDING/ASSIGNED/DELIVERED/CANCELLED): "
        );

        if (orderService.updateOrderStatus(id, status)) {
            System.out.println("Order status updated successfully!");
        } else {
            System.out.println("Order status was not updated.");
        }
    }

    private void deleteOrder() {

        int id = readInt("Enter Order ID to delete: ");

        if (orderService.deleteOrder(id)) {
            System.out.println("Order deleted successfully!");
        } else {
            System.out.println("Order could not be deleted.");
        }
    }

    // VEHICLE MANAGEMENT

    private void addVehicle() {

        System.out.println("\n--- Add New Vehicle ---");

        String registration = readText("Registration number: ");
        String type = readText("Vehicle type: ");
        double capacity = readDouble("Maximum capacity (kg): ");

        vehicleService.addVehicle(registration, type, capacity);
    }

    private void viewAllVehicles() {

        System.out.println("\n--- All Vehicles ---");

        List<Vehicle> vehicles = vehicleService.getAllVehicles();

        if (vehicles.isEmpty()) {
            System.out.println("No vehicles found.");
            return;
        }

        for (Vehicle vehicle : vehicles) {
            printVehicle(vehicle);
        }
    }

    private void searchVehicle() {

        int id = readInt("Enter Vehicle ID: ");

        Vehicle vehicle = vehicleService.getVehicleById(id);

        if (vehicle == null) {
            System.out.println("Vehicle not found.");
            return;
        }

        printVehicle(vehicle);
    }

    private void updateVehicleAvailability() {

        int id = readInt("Enter Vehicle ID: ");

        String status = readText(
            "Enter availability (AVAILABLE/IN_USE/MAINTENANCE): "
        );

        if (vehicleService.updateAvailability(id, status)) {
            System.out.println("Vehicle availability updated!");
        } else {
            System.out.println("Vehicle availability was not updated.");
        }
    }

    private void deleteVehicle() {

        int id = readInt("Enter Vehicle ID to delete: ");

        if (vehicleService.deleteVehicle(id)) {
            System.out.println("Vehicle deleted successfully!");
        } else {
            System.out.println("Vehicle could not be deleted.");
        }
    }

    private void printVehicle(Vehicle vehicle) {

        System.out.println("--------------------------------");
        System.out.println("Vehicle ID: " + vehicle.getVehicleId());
        System.out.println("Registration: " + vehicle.getRegistrationNumber());
        System.out.println("Type: " + vehicle.getVehicleType());
        System.out.println("Capacity: " + vehicle.getCapacity() + " kg");
        System.out.println("Availability: " + vehicle.getAvailability());
    }

    // FIFO OPTIMIZATION

    private void runFifoOptimization() {

        System.out.println("\n--- FIFO Optimization ---");

        viewAllVehicles();

        int vehicleId = readInt("Enter Vehicle ID: ");

        String confirmation = readText(
            "Save the optimized assignments? (Y/N): "
        );

        optimisationService.runFifoOptimization(
            vehicleId,
            confirmation.equalsIgnoreCase("Y")
        );
    }

    // GREEDY OPTIMIZATION

    private void runGreedyOptimization() {

        System.out.println("\n--- Greedy Optimization ---");

        viewAllVehicles();

        int vehicleId = readInt("Enter Vehicle ID: ");

        String confirmation = readText(
            "Save the optimized assignments? (Y/N): "
        );

        optimisationService.runGreedyOptimization(
            vehicleId,
            confirmation.equalsIgnoreCase("Y")
        );
    }

    // KNAPSACK OPTIMIZATION

    private void runKnapsackOptimization() {

        System.out.println("\n--- Knapsack Optimization ---");

        viewAllVehicles();

        int vehicleId = readInt("Enter Vehicle ID: ");

        String confirmation = readText(
            "Save the optimized assignments? (Y/N): "
        );

        optimisationService.runKnapsackOptimization(
            vehicleId,
            confirmation.equalsIgnoreCase("Y")
        );
    }

    // ALGORITHM COMPARISON

    private void compareAlgorithms() {

        System.out.println("\n--- Algorithm Comparison ---");

        viewAllVehicles();

        int vehicleId = readInt("Enter Vehicle ID: ");

        optimisationService.compareAlgorithms(vehicleId);

        int choice = readInt(
            "Choose result to save (1=FIFO, 2=Greedy, 3=Knapsack, 0=Cancel): "
        );

        if (choice == 1) {

            optimisationService.saveSelectedResult(
                vehicleId, "FIFO"
            );

        } else if (choice == 2) {

            optimisationService.saveSelectedResult(
                vehicleId, "GREEDY"
            );

        } else if (choice == 3) {

            optimisationService.saveSelectedResult(
                vehicleId, "KNAPSACK"
            );

        } else {

            System.out.println("No assignments saved.");
        }
    }

    // ROUTE OPTIMIZATION

    private void findShortestRoute() {

        System.out.println("\n--- Shortest Route Finder ---");

        System.out.println("Sample destinations:");
        System.out.println("Bengaluru");
        System.out.println("Mysuru");
        System.out.println("Chennai");
        System.out.println("Hyderabad");
        System.out.println("Mumbai");

        String start = readText("Enter starting destination: ");
        String destination = readText("Enter final destination: ");

        routeOptimizer.displayShortestRoute(start, destination);
    }

    // INPUT HELPERS

    private String readText(String message) {

        System.out.print(message);

        return scanner.nextLine().trim();
    }

    private int readInt(String message) {

        while (true) {

            System.out.print(message);

            try {
                return Integer.parseInt(scanner.nextLine().trim());

            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private double readDouble(String message) {

        while (true) {

            System.out.print(message);

            try {

                double value = Double.parseDouble(
                    scanner.nextLine().trim()
                );

                if (Double.isFinite(value)) {
                    return value;
                }

                System.out.println("Please enter a finite number.");

            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
