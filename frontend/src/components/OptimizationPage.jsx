
import { useEffect, useState } from "react";
import {
  getVehicles,
  getOrders,
  runOptimization,
} from "../services/api";

function OptimizationPage({ algorithm, title, description }) {
  const [vehicles, setVehicles] = useState([]);
  const [orders, setOrders] = useState([]);
  const [vehicleId, setVehicleId] = useState("");
  const [selectedOrderIds, setSelectedOrderIds] = useState([]);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [vehiclesLoading, setVehiclesLoading] = useState(true);
  const [ordersLoading, setOrdersLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  async function loadVehicles() {
    try {
      const data = await getVehicles();
      const availableVehicles = Array.isArray(data)
        ? data.filter((vehicle) => vehicle.availability === "AVAILABLE")
        : [];

      setVehicles(availableVehicles);

      setVehicleId((current) => {
        if (
          current &&
          availableVehicles.some(
            (vehicle) => String(vehicle.vehicleId) === String(current)
          )
        ) {
          return current;
        }

        return availableVehicles.length > 0
          ? String(availableVehicles[0].vehicleId)
          : "";
      });
    } catch (err) {
      setError("Unable to load vehicles: " + err.message);
    } finally {
      setVehiclesLoading(false);
    }
  }

  async function loadOrders() {
    try {
      setOrdersLoading(true);

      const data = await getOrders();

      const pendingOrders = Array.isArray(data)
        ? data.filter(
            (order) =>
              String(order.orderStatus || "").toUpperCase() === "PENDING"
          )
        : [];

      setOrders(pendingOrders);

      setSelectedOrderIds((current) =>
        current.filter((id) =>
          pendingOrders.some(
            (order) => Number(order.orderId) === Number(id)
          )
        )
      );
    } catch (err) {
      setError("Unable to load orders: " + err.message);
    } finally {
      setOrdersLoading(false);
    }
  }

  useEffect(() => {
    loadVehicles();
    loadOrders();
  }, []);

  const selectedVehicle = vehicles.find(
    (vehicle) => String(vehicle.vehicleId) === String(vehicleId)
  );

  function toggleOrder(orderId) {
    setSelectedOrderIds((current) =>
      current.includes(Number(orderId))
        ? current.filter((id) => id !== Number(orderId))
        : [...current, Number(orderId)]
    );

    setResult(null);
    setMessage("");
  }

  function toggleAllOrders() {
    if (selectedOrderIds.length === orders.length) {
      setSelectedOrderIds([]);
    } else {
      setSelectedOrderIds(
        orders.map((order) => Number(order.orderId))
      );
    }

    setResult(null);
    setMessage("");
  }

  async function handleRun(save) {
    if (!vehicleId) {
      setError("Please select an available vehicle.");
      return;
    }

    if (selectedOrderIds.length === 0) {
      setError("Please select at least one pending order.");
      return;
    }

    const selectedIds = [...selectedOrderIds];

    setLoading(true);
    setError("");
    setMessage("");
    setResult(null);

    try {
      const data = await runOptimization(
        algorithm,
        vehicleId,
        save,
        selectedIds
      );

      setResult(data);

      setMessage(
        data.assignmentsSaved
          ? "Optimization completed and assignments saved successfully!"
          : save
            ? "Optimization completed, but assignments were not saved. Check the backend response."
            : "Simulation completed successfully. No assignments were saved."
      );

      if (data.assignmentsSaved) {
        await Promise.all([
          loadVehicles(),
          loadOrders(),
        ]);

        setSelectedOrderIds([]);
      }
    } catch (err) {
      setError(err.message || "Optimization failed.");
    } finally {
      setLoading(false);
    }
  }

  function number(value, digits = 2) {
    const parsed = Number(value);

    if (!Number.isFinite(parsed)) {
      return "0";
    }

    return parsed.toLocaleString("en-IN", {
      minimumFractionDigits: 0,
      maximumFractionDigits: digits,
    });
  }

  const totalWeight = Number(result?.totalWeight || 0);
  const totalVolume = Number(result?.totalVolume || 0);

  const weightCapacity = Number(
    result?.vehicleCapacity ??
    selectedVehicle?.capacity ??
    0
  );

  const volumeCapacity = Number(
    result?.vehicleVolumeCapacity ??
    result?.volumeCapacity ??
    selectedVehicle?.volumeCapacity ??
    0
  );

  const remainingWeight = Number(
    result?.remainingCapacity ??
    Math.max(0, weightCapacity - totalWeight)
  );

  const remainingVolume = Number(
    result?.remainingVolumeCapacity ??
    Math.max(0, volumeCapacity - totalVolume)
  );

  const weightUtilization =
    weightCapacity > 0
      ? (totalWeight / weightCapacity) * 100
      : 0;

  const volumeUtilization =
    volumeCapacity > 0
      ? (totalVolume / volumeCapacity) * 100
      : 0;

  const selectedOrders = Array.isArray(result?.orders)
    ? result.orders
    : [];

  const unassignedOrders = Array.isArray(result?.unassignedOrders)
    ? result.unassignedOrders
    : [];

  const allSelected =
    orders.length > 0 &&
    selectedOrderIds.length === orders.length;

  return (
    <div className="page-container optimization-page">
      <div className="dashboard-heading">
        <div>
          <h2>{title}</h2>
          <p>{description}</p>
        </div>
      </div>

      {error && <p className="error-message">{error}</p>}
      {message && <p className="success-message">{message}</p>}

      <div className="algorithm-panel">
        <h3>Optimization Settings</h3>

        <label htmlFor="optimizationVehicle">
          Select Available Vehicle
        </label>

        <select
          id="optimizationVehicle"
          value={vehicleId}
          onChange={(e) => {
            setVehicleId(e.target.value);
            setResult(null);
          }}
          disabled={vehiclesLoading || loading}
        >
          <option value="">
            {vehiclesLoading
              ? "Loading vehicles..."
              : "-- Select Vehicle --"}
          </option>

          {vehicles.map((vehicle) => (
            <option
              key={vehicle.vehicleId}
              value={vehicle.vehicleId}
            >
              {vehicle.registrationNumber} -{" "}
              {number(vehicle.capacity)} kg /{" "}
              {number(vehicle.volumeCapacity, 3)} m³
            </option>
          ))}
        </select>

        {selectedVehicle && (
          <div className="optimization-vehicle-info">
            <div>
              <span>Weight capacity</span>
              <strong>{number(selectedVehicle.capacity)} kg</strong>
            </div>

            <div>
              <span>Volume capacity</span>
              <strong>
                {number(selectedVehicle.volumeCapacity, 3)} m³
              </strong>
            </div>

            <div>
              <span>Vehicle type</span>
              <strong>{selectedVehicle.vehicleType}</strong>
            </div>
          </div>
        )}

        {!vehiclesLoading && vehicles.length === 0 && (
          <p className="error-message">
            No available vehicles found. Please register or make a
            vehicle available before running optimization.
          </p>
        )}

        <div className="order-selection">
          <div className="order-selection-heading">
            <div>
              <h3>Select Pending Orders</h3>
              <p>
                Choose exactly which orders should be considered.
              </p>
            </div>

            <span className="selected-count">
              {selectedOrderIds.length} selected
            </span>
          </div>

          {ordersLoading ? (
            <p>Loading pending orders...</p>
          ) : orders.length === 0 ? (
            <p className="empty-orders">
              No pending orders available.
            </p>
          ) : (
            <>
              <label className="select-all-option">
                <input
                  type="checkbox"
                  checked={allSelected}
                  onChange={toggleAllOrders}
                  disabled={loading}
                />
                Select All Pending Orders
              </label>

              <div className="order-selection-list">
                {orders.map((order) => (
                  <label
                    key={order.orderId}
                    className="order-selection-item"
                  >
                    <input
                      type="checkbox"
                      checked={selectedOrderIds.includes(
                        Number(order.orderId)
                      )}
                      onChange={() => toggleOrder(order.orderId)}
                      disabled={loading}
                    />

                    <div className="order-selection-details">
                      <strong>
                        Order #{order.orderId} - {order.customerName}
                      </strong>

                      <span>{order.destination}</span>

                      <small>
                        {number(order.weight)} kg ·{" "}
                        {number(order.volume, 3)} m³ · Priority{" "}
                        {order.priority}
                      </small>
                    </div>
                  </label>
                ))}
              </div>
            </>
          )}
        </div>

        <div className="algorithm-actions">
          <button
            type="button"
            onClick={() => handleRun(false)}
            disabled={
              loading ||
              !vehicleId ||
              selectedOrderIds.length === 0 ||
              vehiclesLoading ||
              ordersLoading
            }
          >
            {loading ? "Running..." : "Run Optimization"}
          </button>

          <button
            type="button"
            onClick={() => handleRun(true)}
            disabled={
              loading ||
              !vehicleId ||
              selectedOrderIds.length === 0 ||
              vehiclesLoading ||
              ordersLoading
            }
          >
            {loading ? "Processing..." : "Run and Save Assignments"}
          </button>
        </div>

        <p className="comparison-note">
          Run Optimization performs a simulation without assigning orders.
          Run and Save Assignments attempts to save only the selected orders
          against the chosen vehicle.
        </p>
      </div>

      {result && (
        <div className="result-panel optimization-results">
          <div className="optimization-result-heading">
            <div>
              <span className="algorithm-tag">
                {result.algorithm || algorithm.toUpperCase()}
              </span>

              <h3>{title} Results</h3>
              <p>Delivery allocation and vehicle capacity analysis.</p>
            </div>
          </div>

          <div className="dashboard-grid">
            <div className="dashboard-card">
              <h3>Selected Orders</h3>
              <h2>{result.selectedOrders ?? selectedOrders.length}</h2>
              <p>Orders selected for loading</p>
            </div>

            <div className="dashboard-card">
              <h3>Total Loaded Weight</h3>
              <h2>{number(totalWeight)} kg</h2>
              <p>Weight assigned to vehicle</p>
            </div>

            <div className="dashboard-card">
              <h3>Remaining Weight</h3>
              <h2>{number(remainingWeight)} kg</h2>
              <p>Available weight capacity</p>
            </div>

            <div className="dashboard-card">
              <h3>Execution Time</h3>
              <h2>{number(result.executionTimeMs, 4)} ms</h2>
              <p>Algorithm execution duration</p>
            </div>
          </div>

          <div className="algorithm-details">
            <div>
              <span>Loaded volume</span>
              <h3>{number(totalVolume, 3)} m³</h3>
              <p>Total volume of selected orders</p>
            </div>

            <div>
              <span>Remaining volume</span>
              <h3>{number(remainingVolume, 3)} m³</h3>
              <p>Available volume capacity</p>
            </div>

            <div>
              <span>Total priority score</span>
              <h3>
                {result.totalPriorityScore !== undefined
                  ? number(result.totalPriorityScore, 2)
                  : "—"}
              </h3>
              <p>Combined score of selected orders</p>
            </div>
          </div>

          <div className="optimization-capacity-panel">
            <h3>Vehicle Capacity Utilization</h3>

            <div className="optimization-utilization-item">
              <div className="optimization-utilization-heading">
                <span>Weight utilization</span>
                <strong>{number(weightUtilization, 2)}%</strong>
              </div>

              <div className="comparison-bar-track">
                <div
                  className="comparison-bar-fill"
                  style={{
                    width: `${Math.min(100, Math.max(0, weightUtilization))}%`,
                  }}
                />
              </div>

              <p>
                {number(totalWeight)} kg loaded out of{" "}
                {number(weightCapacity)} kg
              </p>
            </div>

            <div className="optimization-utilization-item">
              <div className="optimization-utilization-heading">
                <span>Volume utilization</span>
                <strong>{number(volumeUtilization, 2)}%</strong>
              </div>

              <div className="comparison-bar-track">
                <div
                  className="comparison-bar-fill"
                  style={{
                    width: `${Math.min(100, Math.max(0, volumeUtilization))}%`,
                  }}
                />
              </div>

              <p>
                {number(totalVolume, 3)} m³ loaded out of{" "}
                {number(volumeCapacity, 3)} m³
              </p>
            </div>
          </div>

          <div className="optimization-run-details">
            <p>
              <strong>Vehicle:</strong>{" "}
              {result.vehicleRegistration ||
                selectedVehicle?.registrationNumber ||
                "—"}
            </p>

            <p>
              <strong>Weight Capacity:</strong>{" "}
              {number(weightCapacity)} kg
            </p>

            <p>
              <strong>Volume Capacity:</strong>{" "}
              {number(volumeCapacity, 3)} m³
            </p>

            <p>
              <strong>Assignments Saved:</strong>{" "}
              {result.assignmentsSaved ? "Yes" : "No"}
            </p>

            <p>
              <strong>Selected Order IDs:</strong>{" "}
              {selectedOrderIds.join(", ")}
            </p>
          </div>

          <h3>Selected Orders</h3>

          {selectedOrders.length === 0 ? (
            <p>No eligible orders selected.</p>
          ) : (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Customer</th>
                    <th>Destination</th>
                    <th>Weight</th>
                    <th>Volume</th>
                    <th>Priority</th>
                  </tr>
                </thead>

                <tbody>
                  {selectedOrders.map((order) => (
                    <tr key={order.orderId}>
                      <td>{order.orderId}</td>
                      <td>{order.customerName}</td>
                      <td>{order.destination}</td>
                      <td>{number(order.weight)} kg</td>
                      <td>{number(order.volume, 3)} m³</td>
                      <td>{order.priority}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {unassignedOrders.length > 0 && (
            <>
              <h3>Unassigned Orders</h3>

              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Order ID</th>
                      <th>Customer</th>
                      <th>Weight</th>
                      <th>Volume</th>
                      <th>Reason</th>
                    </tr>
                  </thead>

                  <tbody>
                    {unassignedOrders.map((order) => (
                      <tr key={order.orderId}>
                        <td>{order.orderId}</td>
                        <td>{order.customerName}</td>
                        <td>{number(order.weight)} kg</td>
                        <td>{number(order.volume, 3)} m³</td>
                        <td>
                          {order.reason ||
                            order.message ||
                            "Not selected by the optimization algorithm"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      )}

      <style>{`
        .order-selection {
          margin-top: 24px;
          padding: 18px;
          border: 1px solid #e5def8;
          border-radius: 12px;
          background: var(--card-bg, #ffffff);
        }

        .order-selection-heading {
          display: flex;
          justify-content: space-between;
          align-items: center;
          flex-wrap: wrap;
          gap: 12px;
          margin-bottom: 16px;
        }

        .order-selection-heading h3 {
          margin: 0;
        }

        .order-selection-heading p {
          margin-top: 5px;
          color: #8b83a5;
          font-size: 13px;
        }

        .selected-count {
          padding: 7px 12px;
          border-radius: 20px;
          background: #f1edff;
          color: #6d45c7;
          font-size: 12px;
          font-weight: 600;
        }

        .select-all-option {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 12px;
          border-bottom: 1px solid #e9e3f5;
          font-weight: 600;
          cursor: pointer;
        }

        .order-selection-list {
          max-height: 320px;
          overflow-y: auto;
        }

        .order-selection-item {
          display: flex;
          align-items: flex-start;
          gap: 12px;
          padding: 13px 12px;
          border-bottom: 1px solid #eeeaf5;
          cursor: pointer;
        }

        .order-selection-item:hover {
          background: #faf8ff;
        }

        .order-selection-item input,
        .select-all-option input {
          width: 17px;
          height: 17px;
          accent-color: #7c3aed;
          flex-shrink: 0;
          margin-top: 3px;
          cursor: pointer;
        }

        .order-selection-details {
          display: flex;
          flex-direction: column;
          gap: 5px;
          min-width: 0;
        }

        .order-selection-details strong {
          color: #44336d;
          font-size: 13px;
        }

        .order-selection-details span {
          color: #777;
          font-size: 12px;
        }

        .order-selection-details small {
          color: #8b83a5;
          font-size: 11px;
        }

        .empty-orders {
          padding: 15px;
          color: #8b83a5;
          text-align: center;
        }

        .algorithm-actions button:disabled {
          opacity: 0.55;
          cursor: not-allowed;
        }

        .optimization-run-details {
          margin-top: 18px;
          padding: 15px;
          border-radius: 10px;
          background: #f8f6ff;
          overflow-wrap: anywhere;
        }

        .optimization-run-details p {
          margin: 7px 0;
          font-size: 13px;
        }

        @media (max-width: 600px) {
          .order-selection {
            padding: 12px;
          }

          .order-selection-heading {
            align-items: flex-start;
          }
        }
      `}</style>
    </div>
  );
}

export default OptimizationPage;
