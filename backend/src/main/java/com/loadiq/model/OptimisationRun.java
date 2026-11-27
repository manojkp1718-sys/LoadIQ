
package com.loadiq.model;

public class OptimisationRun {

    private int runId;
    private String algorithmName;
    private int totalOrders;
    private int assignedOrders;
    private double totalWeight;
    private double totalPriorityScore;
    private double executionTimeMs;
    private String runDate;

    public OptimisationRun(
            String algorithmName,
            int totalOrders,
            int assignedOrders,
            double totalWeight,
            double totalPriorityScore,
            double executionTimeMs) {

        this.algorithmName = algorithmName;
        this.totalOrders = totalOrders;
        this.assignedOrders = assignedOrders;
        this.totalWeight = totalWeight;
        this.totalPriorityScore = totalPriorityScore;
        this.executionTimeMs = executionTimeMs;
    }

    public int getRunId() {
        return runId;
    }

    public void setRunId(int runId) {
        this.runId = runId;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public void setAlgorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public int getAssignedOrders() {
        return assignedOrders;
    }

    public void setAssignedOrders(int assignedOrders) {
        this.assignedOrders = assignedOrders;
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public void setTotalWeight(double totalWeight) {
        this.totalWeight = totalWeight;
    }

    public double getTotalPriorityScore() {
        return totalPriorityScore;
    }

    public void setTotalPriorityScore(double totalPriorityScore) {
        this.totalPriorityScore = totalPriorityScore;
    }

    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(double executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getRunDate() {
        return runDate;
    }

    public void setRunDate(String runDate) {
        this.runDate = runDate;
    }
}
