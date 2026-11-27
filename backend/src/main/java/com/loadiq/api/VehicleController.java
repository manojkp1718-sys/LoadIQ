
package com.loadiq.api;

import com.loadiq.model.Vehicle;
import com.loadiq.service.VehicleService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
public class VehicleController {

    private final VehicleService vehicleService =
            new VehicleService();

    // Get all vehicles
    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles() {

        return ResponseEntity.ok(
                vehicleService.getAllVehicles()
        );
    }

    // Get vehicle by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicleById(
            @PathVariable int id) {

        if (id <= 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid vehicle ID.")
            );
        }

        Vehicle vehicle = vehicleService.getVehicleById(id);

        if (vehicle == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "Vehicle not found.")
            );
        }

        return ResponseEntity.ok(vehicle);
    }

    // Add a new vehicle with weight and volume capacity
    @PostMapping
    public ResponseEntity<Map<String, String>> addVehicle(
            @RequestBody(required = false) VehicleRequest request) {

        if (request == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Vehicle details are required.")
            );
        }

        if (request.registrationNumber() == null
                || request.registrationNumber().isBlank()
                || request.vehicleType() == null
                || request.vehicleType().isBlank()
                || !Double.isFinite(request.capacity())
                || request.capacity() <= 0
                || !Double.isFinite(request.volumeCapacity())
                || request.volumeCapacity() <= 0) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Invalid vehicle details. Weight and volume capacities must be positive."
                    )
            );
        }

        boolean added = vehicleService.addVehicle(
                request.registrationNumber(),
                request.vehicleType(),
                request.capacity(),
                request.volumeCapacity()
        );

        if (!added) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                            "message",
                            "Vehicle could not be added. Check registration number."
                    )
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("message", "Vehicle added successfully!")
        );
    }

    // Update vehicle availability
    @PatchMapping("/{id}/availability")
    public ResponseEntity<Map<String, String>> updateAvailability(
            @PathVariable int id,
            @RequestBody(required = false) AvailabilityRequest request) {

        if (id <= 0 || request == null
                || request.availability() == null
                || request.availability().isBlank()) {

            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid availability request.")
            );
        }

        String status = request.availability()
                .trim()
                .toUpperCase();

        if (!status.equals("AVAILABLE")
                && !status.equals("MAINTENANCE")) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Manual status changes only allow AVAILABLE or MAINTENANCE. IN_USE is managed by assignments."
                    )
            );
        }

        Vehicle vehicle = vehicleService.getVehicleById(id);

        if (vehicle == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "Vehicle not found.")
            );
        }

        boolean updated = vehicleService.updateAvailability(
                id, status
        );

        if (!updated) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                            "message",
                            "Availability change rejected. Check the vehicle's current status and active assignments."
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of("message", "Vehicle availability updated successfully!")
        );
    }

    // Delete vehicle
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteVehicle(
            @PathVariable int id) {

        if (id <= 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid vehicle ID.")
            );
        }

        Vehicle vehicle = vehicleService.getVehicleById(id);

        if (vehicle == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "Vehicle not found.")
            );
        }

        boolean deleted = vehicleService.deleteVehicle(id);

        if (!deleted) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                            "message",
                            "Vehicle cannot be deleted because it is in use or has assignment history."
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of("message", "Vehicle deleted successfully!")
        );
    }

    public record VehicleRequest(
            String registrationNumber,
            String vehicleType,
            double capacity,
            double volumeCapacity
    ) {}

    public record AvailabilityRequest(
            String availability
    ) {}
}
