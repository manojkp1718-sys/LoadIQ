package com.loadiq.api;

import com.loadiq.model.Order;
import com.loadiq.service.OrderService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
public class OrderController {

    private final OrderService orderService =
            new OrderService();

    // Get all orders
    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    // Get an order by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(
            @PathVariable int id) {

        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid order ID."
                    ));
        }

        Order order = orderService.getOrderById(id);

        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Order not found."
                    ));
        }

        return ResponseEntity.ok(order);
    }

    // Add a new order with weight and volume
    @PostMapping
    public ResponseEntity<Map<String, String>> addOrder(
            @RequestBody(required = false)
            OrderRequest request) {

        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Order details are required."
                    ));
        }

        if (request.customerName() == null
                || request.customerName().isBlank()
                || request.destination() == null
                || request.destination().isBlank()
                || !Double.isFinite(request.weight())
                || request.weight() <= 0
                || !Double.isFinite(request.volume())
                || request.volume() <= 0
                || request.priority() < 1
                || request.priority() > 5
                || request.deadline() == null
                || request.deadline().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid order details. Weight and volume "
                                    + "must be positive, and priority "
                                    + "must be between 1 and 5."
                    ));
        }

        boolean success = orderService.addOrder(
                request.customerName(),
                request.destination(),
                request.weight(),
                request.volume(),
                request.priority(),
                request.deadline()
        );

        if (!success) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid order details or failed to add order."
                    ));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message",
                        "Order added successfully!"
                ));
    }

    // Geocode existing orders missing coordinates.
    // This endpoint must be triggered manually.
    @PostMapping("/geocode-missing")
    public ResponseEntity<?> geocodeMissingCoordinates() {

        try {
            OrderService.GeocodingSummary summary =
                    orderService.geocodeMissingCoordinates();

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Geocoding process completed.",
                            "totalMissing",
                            summary.totalMissing(),
                            "processed",
                            summary.processed(),
                            "successful",
                            summary.successful(),
                            "failed",
                            summary.failed()
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(Map.of(
                            "message",
                            "Unable to complete geocoding process."
                    ));
        }
    }

    // Update order status
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateOrderStatus(
            @PathVariable int id,
            @RequestBody(required = false)
            StatusRequest request) {

        if (id <= 0 || request == null
                || request.status() == null
                || request.status().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "A valid order ID and status are required."
                    ));
        }

        boolean success = orderService.updateOrderStatus(
                id,
                request.status()
        );

        if (!success) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Order not found or status transition "
                                    + "is not allowed."
                    ));
        }

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Order status updated successfully!"
                )
        );
    }

    // Delete an order
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteOrder(
            @PathVariable int id) {

        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid order ID."
                    ));
        }

        boolean success = orderService.deleteOrder(id);

        if (!success) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Order could not be deleted. It may have "
                                    + "assignment history or may not exist."
                    ));
        }

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Order deleted successfully!"
                )
        );
    }

    public record OrderRequest(
            String customerName,
            String destination,
            double weight,
            double volume,
            int priority,
            String deadline
    ) {}

    public record StatusRequest(String status) {}
}