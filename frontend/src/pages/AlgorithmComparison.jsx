
import { useEffect, useState } from "react";
import { getVehicles, compareAlgorithms } from "../services/api";

function AlgorithmComparison() {
  const [vehicles, setVehicles] = useState([]);
  const [vehicleId, setVehicleId] = useState("");
  const [comparison, setComparison] = useState(null);
  const [loadingVehicles, setLoadingVehicles] = useState(true);
  const [loadingComparison, setLoadingComparison] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    loadVehicles();
  }, []);

  async function loadVehicles() {
    try {
      setLoadingVehicles(true);
      setError("");

      const data = await getVehicles();
      const list = Array.isArray(data)
        ? data
        : data.vehicles || data.data || [];

      setVehicles(list);

      // Prefer an available vehicle for benchmarking.
      const available = list.filter(
        (vehicle) => vehicle.availability === "AVAILABLE"
      );

      const firstVehicle = available[0] || list[0];

      if (firstVehicle) {
        const id =
          firstVehicle.vehicleId ??
          firstVehicle.vehicle_id ??
          firstVehicle.id;

        setVehicleId(String(id));
      }
    } catch (err) {
      setError(err.message || "Unable to load vehicles.");
    } finally {
      setLoadingVehicles(false);
    }
  }

  async function handleCompare() {
    if (!vehicleId) {
      setError("Please select a vehicle.");
      return;
    }

    try {
      setLoadingComparison(true);
      setError("");
      setComparison(null);

      const result = await compareAlgorithms(vehicleId);
      setComparison(result);
    } catch (err) {
      setError(err.message || "Algorithm comparison failed.");
    } finally {
      setLoadingComparison(false);
    }
  }

  function formatNumber(value, digits = 2) {
    const number = Number(value);

    if (!Number.isFinite(number)) {
      return "0";
    }

    return number.toLocaleString("en-IN", {
      minimumFractionDigits: 0,
      maximumFractionDigits: digits,
    });
  }

  function getPercentage(value, used, capacity) {
    const supplied = Number(value);

    if (value !== null && value !== undefined &&
        Number.isFinite(supplied)) {
      return supplied;
    }

    const totalCapacity = Number(capacity);

    if (!Number.isFinite(totalCapacity) || totalCapacity <= 0) {
      return 0;
    }

    return (Number(used || 0) / totalCapacity) * 100;
  }

  const results = comparison?.results || [];

  const selectedVehicle = vehicles.find(
    (vehicle) =>
      String(
        vehicle.vehicleId ??
        vehicle.vehicle_id ??
        vehicle.id
      ) === String(vehicleId)
  );

  const vehicleWeightCapacity = Number(
    comparison?.vehicleCapacity ??
    selectedVehicle?.capacity ??
    0
  );

  const vehicleVolumeCapacity = Number(
    comparison?.vehicleVolumeCapacity ??
    selectedVehicle?.volumeCapacity ??
    0
  );

  const fastestAlgorithm = results.length
    ? [...results].sort(
        (a, b) =>
          Number(a.executionTimeMs) - Number(b.executionTimeMs)
      )[0]
    : null;

  const highestScoreAlgorithm = results.length
    ? [...results].sort(
        (a, b) =>
          Number(b.totalPriorityScore) -
          Number(a.totalPriorityScore)
      )[0]
    : null;

  const highestWeightUtilizationAlgorithm = results.length
    ? [...results].sort(
        (a, b) =>
          getPercentage(
            b.weightUtilization,
            b.totalWeight,
            vehicleWeightCapacity
          ) -
          getPercentage(
            a.weightUtilization,
            a.totalWeight,
            vehicleWeightCapacity
          )
      )[0]
    : null;

  const highestVolumeUtilizationAlgorithm = results.length
    ? [...results].sort(
        (a, b) =>
          getPercentage(
            b.volumeUtilization,
            b.totalVolume,
            vehicleVolumeCapacity
          ) -
          getPercentage(
            a.volumeUtilization,
            a.totalVolume,
            vehicleVolumeCapacity
          )
      )[0]
    : null;

  const maxExecutionTime = Math.max(
    ...results.map((item) => Number(item.executionTimeMs || 0)),
    0.000001
  );

  const algorithmColors = {
    FIFO: "#2563eb",
    GREEDY: "#16a34a",
    KNAPSACK: "#9333ea",
  };

  function renderBar(label, value, color) {
    const safeValue = Math.min(
      100,
      Math.max(0, Number(value) || 0)
    );

    return (
      <div className="comparison-bar-group" key={label}>
        <div className="comparison-bar-label">
          <span>{label}</span>
          <strong>{formatNumber(safeValue)}%</strong>
        </div>

        <div className="comparison-bar-track">
          <div
            className="comparison-bar-fill"
            style={{
              width: `${safeValue}%`,
              background: color,
            }}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="comparison-heading">
        <div>
          <h2>Algorithm Comparison</h2>
          <p>
            Compare FIFO, Greedy and Knapsack optimization
            using the same pending orders and vehicle capacity.
          </p>
        </div>
      </div>

      <div className="comparison-control-card">
        <div className="comparison-control-info">
          <h3>Run Algorithm Benchmark</h3>
          <p>
            Select an available vehicle to simulate all three
            algorithms. This comparison does not assign orders
            or reserve the selected vehicle.
          </p>
        </div>

        <div className="comparison-controls">
          <div className="comparison-select-group">
            <label htmlFor="comparisonVehicle">
              Select Available Vehicle
            </label>

            <select
              id="comparisonVehicle"
              value={vehicleId}
              onChange={(e) => {
                setVehicleId(e.target.value);
                setComparison(null);
                setError("");
              }}
              disabled={loadingVehicles || vehicles.length === 0}
            >
              {loadingVehicles && (
                <option value="">Loading vehicles...</option>
              )}

              {!loadingVehicles && vehicles.length === 0 && (
                <option value="">No vehicles available</option>
              )}

              {vehicles.map((vehicle) => {
                const id =
                  vehicle.vehicleId ??
                  vehicle.vehicle_id ??
                  vehicle.id;

                const registration =
                  vehicle.registrationNumber ??
                  vehicle.registration_number ??
                  vehicle.registration ??
                  `Vehicle ${id}`;

                const weight = Number(vehicle.capacity || 0);
                const volume = Number(vehicle.volumeCapacity || 0);

                return (
                  <option key={id} value={id}>
                    {registration} - {formatNumber(weight)} kg /{" "}
                    {formatNumber(volume, 3)} m³ (
                    {vehicle.availability || "UNKNOWN"})
                  </option>
                );
              })}
            </select>
          </div>

          <button
            className="comparison-run-button"
            type="button"
            onClick={handleCompare}
            disabled={
              loadingComparison ||
              loadingVehicles ||
              !vehicleId
            }
          >
            {loadingComparison
              ? "Comparing Algorithms..."
              : "Run Comparison"}
          </button>
        </div>

        {selectedVehicle && (
          <div className="comparison-vehicle-info">
            <span>
              Vehicle:{" "}
              <strong>
                {selectedVehicle.registrationNumber ||
                  selectedVehicle.registration_number ||
                  "Selected vehicle"}
              </strong>
            </span>

            <span>
              Weight capacity:{" "}
              <strong>
                {formatNumber(selectedVehicle.capacity)} kg
              </strong>
            </span>

            <span>
              Volume capacity:{" "}
              <strong>
                {formatNumber(selectedVehicle.volumeCapacity, 3)} m³
              </strong>
            </span>

            <span>
              Status:{" "}
              <strong>
                {selectedVehicle.availability ||
                  selectedVehicle.status ||
                  "Not specified"}
              </strong>
            </span>
          </div>
        )}
      </div>

      {error && (
        <div className="comparison-error">
          {error}
        </div>
      )}

      {loadingComparison && (
        <div className="comparison-loading">
          Running FIFO, Greedy and Knapsack algorithms...
        </div>
      )}

      {comparison && (
        <>
          <div className="comparison-section-heading">
            <div>
              <h3>Comparison Overview</h3>
              <p>
                Results based on the selected vehicle and
                current pending orders.
              </p>
            </div>
          </div>

          <div className="comparison-summary-grid">
            <div className="comparison-summary-card">
              <span>Total Pending Orders</span>
              <strong>{comparison.totalPendingOrders}</strong>
            </div>

            <div className="comparison-summary-card">
              <span>Weight Capacity</span>
              <strong>
                {formatNumber(vehicleWeightCapacity)} kg
              </strong>
            </div>

            <div className="comparison-summary-card">
              <span>Volume Capacity</span>
              <strong>
                {formatNumber(vehicleVolumeCapacity, 3)} m³
              </strong>
            </div>

            <div className="comparison-summary-card">
              <span>Algorithms Compared</span>
              <strong>{results.length}</strong>
            </div>
          </div>

          <div className="comparison-section-heading">
            <div>
              <h3>Algorithm Performance</h3>
              <p>
                Detailed results for each optimization algorithm.
              </p>
            </div>
          </div>

          <div className="algorithm-result-grid">
            {results.map((result) => {
              const weightUtilization = getPercentage(
                result.weightUtilization,
                result.totalWeight,
                vehicleWeightCapacity
              );

              const volumeUtilization = getPercentage(
                result.volumeUtilization,
                result.totalVolume,
                vehicleVolumeCapacity
              );

              return (
                <div
                  className="algorithm-result-card"
                  key={result.algorithm}
                  style={{
                    borderTop: `4px solid ${
                      algorithmColors[result.algorithm] || "#64748b"
                    }`,
                  }}
                >
                  <div className="algorithm-card-heading">
                    <h3>{result.algorithm}</h3>

                    {fastestAlgorithm?.algorithm === result.algorithm && (
                      <span className="comparison-tag">
                        Fastest
                      </span>
                    )}
                  </div>

                  <div className="algorithm-main-metric">
                    <span>Selected Orders</span>
                    <strong>{result.selectedOrders}</strong>
                  </div>

                  <div className="algorithm-metric-row">
                    <span>Total Weight</span>
                    <strong>
                      {formatNumber(result.totalWeight)} kg
                    </strong>
                  </div>

                  <div className="algorithm-metric-row">
                    <span>Total Volume</span>
                    <strong>
                      {formatNumber(result.totalVolume, 3)} m³
                    </strong>
                  </div>

                  <div className="algorithm-metric-row">
                    <span>Priority Score</span>
                    <strong>
                      {formatNumber(result.totalPriorityScore)}
                    </strong>
                  </div>

                  <div className="algorithm-metric-row">
                    <span>Remaining Weight</span>
                    <strong>
                      {formatNumber(result.remainingCapacity)} kg
                    </strong>
                  </div>

                  <div className="algorithm-metric-row">
                    <span>Remaining Volume</span>
                    <strong>
                      {formatNumber(result.remainingVolumeCapacity, 3)} m³
                    </strong>
                  </div>

                  <div className="algorithm-metric-row">
                    <span>Weight Utilization</span>
                    <strong>
                      {formatNumber(weightUtilization)}%
                    </strong>
                  </div>

                  <div className="algorithm-metric-row">
                    <span>Volume Utilization</span>
                    <strong>
                      {formatNumber(volumeUtilization)}%
                    </strong>
                  </div>

                  <div className="algorithm-metric-row">
                    <span>Assignment Rate</span>
                    <strong>
                      {formatNumber(result.assignmentRate)}%
                    </strong>
                  </div>

                  <div className="algorithm-metric-row">
                    <span>Execution Time</span>
                    <strong>
                      {formatNumber(result.executionTimeMs, 4)} ms
                    </strong>
                  </div>
                </div>
              );
            })}
          </div>

          <div className="comparison-chart-grid">
            <div className="comparison-chart-card">
              <h3>Execution Time Comparison</h3>
              <p>
                Lower execution time indicates faster measured execution.
              </p>

              {results.map((result) => {
                const percentage =
                  (Number(result.executionTimeMs) /
                    maxExecutionTime) *
                  100;

                return (
                  <div
                    className="comparison-bar-group"
                    key={result.algorithm}
                  >
                    <div className="comparison-bar-label">
                      <span>{result.algorithm}</span>
                      <strong>
                        {formatNumber(result.executionTimeMs, 4)} ms
                      </strong>
                    </div>

                    <div className="comparison-bar-track">
                      <div
                        className="comparison-bar-fill"
                        style={{
                          width: `${Math.max(percentage, 1)}%`,
                          background:
                            algorithmColors[result.algorithm],
                        }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="comparison-chart-card">
              <h3>Weight Capacity Utilization</h3>
              <p>
                Percentage of vehicle weight capacity used.
              </p>

              {results.map((result) =>
                renderBar(
                  result.algorithm,
                  getPercentage(
                    result.weightUtilization,
                    result.totalWeight,
                    vehicleWeightCapacity
                  ),
                  algorithmColors[result.algorithm]
                )
              )}
            </div>

            <div className="comparison-chart-card">
              <h3>Volume Capacity Utilization</h3>
              <p>
                Percentage of vehicle volume capacity used.
              </p>

              {results.map((result) =>
                renderBar(
                  result.algorithm,
                  getPercentage(
                    result.volumeUtilization,
                    result.totalVolume,
                    vehicleVolumeCapacity
                  ),
                  algorithmColors[result.algorithm]
                )
              )}
            </div>

            <div className="comparison-chart-card">
              <h3>Assignment Rate</h3>
              <p>
                Percentage of pending orders selected by each algorithm.
              </p>

              {results.map((result) =>
                renderBar(
                  result.algorithm,
                  result.assignmentRate,
                  algorithmColors[result.algorithm]
                )
              )}
            </div>
          </div>

          <div className="comparison-section-heading">
            <div>
              <h3>Selected Order IDs</h3>
              <p>
                Orders selected by each algorithm during this simulation.
              </p>
            </div>
          </div>

          <div className="algorithm-result-grid">
            {results.map((result) => (
              <div
                className="comparison-insight-card"
                key={result.algorithm}
              >
                <span>{result.algorithm} selected orders</span>
                <strong>
                  {result.selectedOrderIds?.length || 0} orders
                </strong>
                <p>
                  {result.selectedOrderIds?.length
                    ? result.selectedOrderIds.join(", ")
                    : "No order IDs returned"}
                </p>
              </div>
            ))}
          </div>

          <div className="comparison-section-heading">
            <div>
              <h3>Performance Insights</h3>
              <p>
                Highlights based on the current comparison results.
              </p>
            </div>
          </div>

          <div className="comparison-insights-grid">
            <div className="comparison-insight-card">
              <span>Fastest Measured Algorithm</span>
              <strong>
                {fastestAlgorithm?.algorithm || "N/A"}
              </strong>
              <p>
                {fastestAlgorithm
                  ? `${formatNumber(
                      fastestAlgorithm.executionTimeMs,
                      4
                    )} ms median execution time`
                  : "No result available"}
              </p>
            </div>

            <div className="comparison-insight-card">
              <span>Highest Priority Score</span>
              <strong>
                {highestScoreAlgorithm?.algorithm || "N/A"}
              </strong>
              <p>
                {highestScoreAlgorithm
                  ? `${formatNumber(
                      highestScoreAlgorithm.totalPriorityScore
                    )} total priority score`
                  : "No result available"}
              </p>
            </div>

            <div className="comparison-insight-card">
              <span>Highest Weight Utilization</span>
              <strong>
                {highestWeightUtilizationAlgorithm?.algorithm || "N/A"}
              </strong>
              <p>
                {highestWeightUtilizationAlgorithm
                  ? `${formatNumber(
                      getPercentage(
                        highestWeightUtilizationAlgorithm.weightUtilization,
                        highestWeightUtilizationAlgorithm.totalWeight,
                        vehicleWeightCapacity
                      )
                    )}% weight capacity used`
                  : "No result available"}
              </p>
            </div>

            <div className="comparison-insight-card">
              <span>Highest Volume Utilization</span>
              <strong>
                {highestVolumeUtilizationAlgorithm?.algorithm || "N/A"}
              </strong>
              <p>
                {highestVolumeUtilizationAlgorithm
                  ? `${formatNumber(
                      getPercentage(
                        highestVolumeUtilizationAlgorithm.volumeUtilization,
                        highestVolumeUtilizationAlgorithm.totalVolume,
                        vehicleVolumeCapacity
                      )
                    )}% volume capacity used`
                  : "No result available"}
              </p>
            </div>
          </div>

          <div className="comparison-note">
            <strong>Benchmark note:</strong> Execution times are
            median values from 10 measured runs after 3 warm-up runs.
            These values can vary with hardware, JVM activity and
            dataset size. A larger and more varied order dataset is
            needed for a meaningful algorithm performance evaluation.
          </div>
        </>
      )}
    </div>
  );
}

export default AlgorithmComparison;
