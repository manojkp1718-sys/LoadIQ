
package com.loadiq.algorithm;

import java.util.*;

public class RouteOptimizer {

    private final Map<String, Map<String, Double>> graph =
            new HashMap<>();

    // Add a bidirectional route
    public void addRoute(
            String source,
            String destination,
            double distance) {

        if (source == null || destination == null
                || source.isBlank() || destination.isBlank()) {
            throw new IllegalArgumentException(
                    "Destination names cannot be empty."
            );
        }

        if (!Double.isFinite(distance) || distance <= 0) {
            throw new IllegalArgumentException(
                    "Distance must be positive and finite."
            );
        }

        source = source.trim();
        destination = destination.trim();

        if (source.equalsIgnoreCase(destination)) {
            throw new IllegalArgumentException(
                    "Source and destination must be different."
            );
        }

        source = getCanonicalCity(source);
        destination = getCanonicalCity(destination);

        graph.putIfAbsent(source, new HashMap<>());
        graph.putIfAbsent(destination, new HashMap<>());

        graph.get(source).put(destination, distance);
        graph.get(destination).put(source, distance);
    }

    // Find existing city name without case sensitivity
    private String getCanonicalCity(String city) {

        for (String existing : graph.keySet()) {
            if (existing.equalsIgnoreCase(city)) {
                return existing;
            }
        }

        return city;
    }

    // Dijkstra's shortest path algorithm
    public List<String> findShortestRoute(
            String start,
            String destination) {

        if (start == null || destination == null
                || start.isBlank() || destination.isBlank()) {
            return Collections.emptyList();
        }

        start = getCanonicalCity(start.trim());
        destination = getCanonicalCity(destination.trim());

        if (!graph.containsKey(start)
                || !graph.containsKey(destination)) {
            return Collections.emptyList();
        }

        if (start.equals(destination)) {
            return List.of(start);
        }

        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();

        for (String city : graph.keySet()) {
            distances.put(city, Double.POSITIVE_INFINITY);
        }

        distances.put(start, 0.0);

        PriorityQueue<CityDistance> queue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                CityDistance::getDistance
                        )
                );

        queue.add(new CityDistance(start, 0.0));

        Set<String> visited = new HashSet<>();

        while (!queue.isEmpty()) {

            CityDistance entry = queue.poll();
            String current = entry.getCity();

            if (visited.contains(current)) {
                continue;
            }

            visited.add(current);

            if (current.equals(destination)) {
                break;
            }

            for (Map.Entry<String, Double> edge :
                    graph.get(current).entrySet()) {

                String neighbour = edge.getKey();

                if (visited.contains(neighbour)) {
                    continue;
                }

                double newDistance =
                        distances.get(current) + edge.getValue();

                if (newDistance < distances.get(neighbour)) {

                    distances.put(neighbour, newDistance);
                    previous.put(neighbour, current);

                    queue.add(
                            new CityDistance(neighbour, newDistance)
                    );
                }
            }
        }

        if (distances.get(destination)
                == Double.POSITIVE_INFINITY) {
            return Collections.emptyList();
        }

        List<String> route = new ArrayList<>();

        String current = destination;

        while (current != null) {
            route.add(current);
            current = previous.get(current);
        }

        Collections.reverse(route);

        return route;
    }

    // Calculate shortest distance
    public double getShortestDistance(
            String start,
            String destination) {

        List<String> route =
                findShortestRoute(start, destination);

        if (route.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }

        return calculateRouteDistance(route);
    }

    // Calculate distance for a complete route
    public double calculateRouteDistance(List<String> route) {

        if (route == null || route.size() < 2) {
            return 0;
        }

        double totalDistance = 0;

        for (int i = 0; i < route.size() - 1; i++) {

            String current =
                    getCanonicalCity(route.get(i));

            String next =
                    getCanonicalCity(route.get(i + 1));

            Map<String, Double> neighbours = graph.get(current);

            if (neighbours == null
                    || !neighbours.containsKey(next)) {
                return Double.POSITIVE_INFINITY;
            }

            totalDistance += neighbours.get(next);
        }

        return totalDistance;
    }

    // Multi-stop delivery routing using nearest neighbour
    public List<String> optimizeMultipleStops(
            String start,
            List<String> deliveryStops) {

        if (start == null || start.isBlank()
                || deliveryStops == null
                || deliveryStops.isEmpty()) {
            return Collections.emptyList();
        }

        String current = getCanonicalCity(start.trim());

        if (!graph.containsKey(current)) {
            return Collections.emptyList();
        }

        Set<String> unvisited = new LinkedHashSet<>();

        for (String stop : deliveryStops) {

            if (stop == null || stop.isBlank()) {
                throw new IllegalArgumentException(
                        "Delivery locations cannot be empty."
                );
            }

            String city = getCanonicalCity(stop.trim());

            if (!graph.containsKey(city)) {
                return Collections.emptyList();
            }

            if (!city.equals(current)) {
                unvisited.add(city);
            }
        }

        List<String> finalRoute = new ArrayList<>();
        finalRoute.add(current);

        while (!unvisited.isEmpty()) {

            String nearestCity = null;
            double shortestDistance = Double.POSITIVE_INFINITY;

            for (String stop : unvisited) {

                double distance =
                        getShortestDistance(current, stop);

                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    nearestCity = stop;
                }
            }

            if (nearestCity == null
                    || !Double.isFinite(shortestDistance)) {
                return Collections.emptyList();
            }

            List<String> segment =
                    findShortestRoute(current, nearestCity);

            if (segment.isEmpty()) {
                return Collections.emptyList();
            }

            finalRoute.addAll(
                    segment.subList(1, segment.size())
            );

            current = nearestCity;
            unvisited.remove(nearestCity);
        }

        return finalRoute;
    }

    // Display shortest route
    public void displayShortestRoute(
            String start,
            String destination) {

        List<String> route =
                findShortestRoute(start, destination);

        System.out.println("\n================================");
        System.out.println("       ROUTE OPTIMIZATION");
        System.out.println("================================");

        if (route.isEmpty()) {
            System.out.println(
                    "No route found between destinations."
            );
            return;
        }

        System.out.println("Starting Point: " + start);
        System.out.println("Destination: " + destination);
        System.out.println("\nShortest Route:");

        for (int i = 0; i < route.size(); i++) {

            System.out.print(route.get(i));

            if (i < route.size() - 1) {
                System.out.print(" -> ");
            }
        }

        System.out.println();

        System.out.println(
                "Total Distance: "
                        + getShortestDistance(start, destination)
                        + " km"
        );

        System.out.println("================================");
    }

    // Sample route graph
    public void loadSampleRoutes() {

        addRoute("Bengaluru", "Mysuru", 145);
        addRoute("Bengaluru", "Chennai", 350);
        addRoute("Bengaluru", "Hyderabad", 570);
        addRoute("Mysuru", "Chennai", 480);
        addRoute("Chennai", "Hyderabad", 630);
        addRoute("Mysuru", "Mumbai", 950);
        addRoute("Hyderabad", "Mumbai", 710);
    }

    private static class CityDistance {

        private final String city;
        private final double distance;

        public CityDistance(String city, double distance) {
            this.city = city;
            this.distance = distance;
        }

        public String getCity() {
            return city;
        }

        public double getDistance() {
            return distance;
        }
    }
}
