
import { useEffect, useState } from "react";
import DashboardCard from "../components/DashboardCard";
import { getDashboardData } from "../services/api";

function Dashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadDashboard() {
    try {
      setLoading(true);
      setError("");

      const result = await getDashboardData();
      setData(result || {});
    } catch (err) {
      setError(err.message || "Failed to load dashboard");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadDashboard();
  }, []);

  function formatDateTime(value) {
    if (!value) return "—";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return date.toLocaleString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: true,
    });
  }

  function getAlgorithmClass(algorithm) {
    switch (algorithm?.toUpperCase()) {
      case "FIFO":
        return "algorithm-badge algorithm-fifo";
      case "GREEDY":
        return "algorithm-badge algorithm-greedy";
      case "KNAPSACK":
        return "algorithm-badge algorithm-knapsack";
      default:
        return "algorithm-badge";
    }
  }

  const number = (value) => Number(value || 0);

  if (loading) {
    return (
      <div className="page-container">
        <h2>LoadIQ Dashboard</h2>
        <p>Loading live dashboard statistics...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-container">
        <h2>LoadIQ Dashboard</h2>
        <p className="error-message">{error}</p>
        <button onClick={loadDashboard}>Try Again</button>
      </div>
    );
  }

  const recentOptimizations = data?.recentOptimizations || [];
  const recentRoutes = data?.recentRoutes || [];

  return (
    <div className="page-container">

      {/* DASHBOARD HEADER */}

      <div className="dashboard-heading">
        <div>
          <h2>LoadIQ Dashboard</h2>
          <p>
            Intelligent Logistics Management and Delivery Optimization
          </p>
        </div>

        <button onClick={loadDashboard} disabled={loading}>
          Refresh Dashboard
        </button>
      </div>

      {/* ORDER OVERVIEW */}

      <h3>Order Overview</h3>

      <div className="dashboard-grid">
        <DashboardCard
          title="Total Orders"
          value={data.totalOrders ?? 0}
          description="All recorded orders"
        />

        <DashboardCard
          title="Pending Orders"
          value={data.pendingOrders ?? 0}
          description="Waiting for assignment"
        />

        <DashboardCard
          title="Assigned Orders"
          value={data.assignedOrders ?? 0}
          description="Assigned to vehicles"
        />

        <DashboardCard
          title="Delivered Orders"
          value={data.deliveredOrders ?? 0}
          description="Successfully delivered"
        />

        <DashboardCard
          title="Cancelled Orders"
          value={data.cancelledOrders ?? 0}
          description="Cancelled orders"
        />
      </div>

      {/* VEHICLE OVERVIEW */}

      <h3>Vehicle Overview</h3>

      <div className="dashboard-grid">
        <DashboardCard
          title="Total Vehicles"
          value={data.totalVehicles ?? 0}
          description="Registered vehicles"
        />

        <DashboardCard
          title="Available Vehicles"
          value={data.availableVehicles ?? 0}
          description="Ready for assignment"
        />

        <DashboardCard
          title="Vehicles In Use"
          value={data.vehiclesInUse ?? 0}
          description="Currently assigned"
        />

        <DashboardCard
          title="Vehicles In Maintenance"
          value={data.vehiclesInMaintenance ?? 0}
          description="Unavailable for delivery"
        />
      </div>

      {/* ROUTE ANALYTICS */}

      <h3>Route Analytics</h3>

      <div className="dashboard-grid">
        <DashboardCard
          title="Total Routes"
          value={data.totalRoutes ?? 0}
          description="Successfully saved routes"
        />

        <DashboardCard
          title="Total Distance"
          value={`${number(data.totalDistanceKm).toFixed(2)} km`}
          description="Combined route distance"
        />

        <DashboardCard
          title="Average Distance"
          value={`${number(data.averageDistanceKm).toFixed(2)} km`}
          description="Average distance per route"
        />

        <DashboardCard
          title="Average Duration"
          value={`${number(data.averageDurationMinutes).toFixed(1)} min`}
          description="Estimated travel duration"
        />
      </div>

      {/* RECENT ROUTES */}

      <div className="table-container">
        <h3>Recent Route Activity</h3>

        {recentRoutes.length === 0 ? (
          <p>No saved routes yet. Optimize a route to see it here.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Route ID</th>
                <th>Starting Location</th>
                <th>Distance</th>
                <th>Duration</th>
                <th>Created At</th>
              </tr>
            </thead>

            <tbody>
              {recentRoutes.map((route) => (
                <tr key={route.routeId}>
                  <td>#{route.routeId}</td>
                  <td>{route.startLocation || "—"}</td>
                  <td>
                    {number(route.distanceKm).toFixed(2)} km
                  </td>
                  <td>
                    {number(route.durationMinutes).toFixed(1)} min
                  </td>
                  <td>{formatDateTime(route.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* RECENT OPTIMIZATION ACTIVITY */}

      <div className="table-container">
        <h3>Recent Optimization Activity</h3>

        {recentOptimizations.length === 0 ? (
          <p>No saved optimization runs yet.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Run ID</th>
                <th>Algorithm</th>
                <th>Total Orders</th>
                <th>Assigned Orders</th>
                <th>Total Weight</th>
                <th>Total Volume</th>
                <th>Priority Score</th>
                <th>Execution Time</th>
                <th>Run Date</th>
              </tr>
            </thead>

            <tbody>
              {recentOptimizations.map((run) => (
                <tr key={run.runId}>
                  <td>#{run.runId}</td>

                  <td>
                    <span className={getAlgorithmClass(run.algorithmName)}>
                      {run.algorithmName || "UNKNOWN"}
                    </span>
                  </td>

                  <td>{run.totalOrders ?? 0}</td>
                  <td>{run.assignedOrders ?? 0}</td>

                  <td>
                    {number(run.totalWeight).toFixed(2)} kg
                  </td>

                  <td>
                    {run.totalVolume != null
                      ? `${number(run.totalVolume).toFixed(3)} m³`
                      : "—"}
                  </td>

                  <td>
                    {number(run.totalPriorityScore).toFixed(2)}
                  </td>

                  <td>
                    {number(run.executionTimeMs).toFixed(3)} ms
                  </td>

                  <td>{formatDateTime(run.runDate)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* COMPONENT STYLES */}

      <style>{`
        .dashboard-heading {
          display: flex;
          justify-content: space-between;
          align-items: center;
          gap: 16px;
          flex-wrap: wrap;
          margin-bottom: 24px;
        }

        .dashboard-heading p {
          color: #8b83a5;
          font-size: 13px;
          margin-top: 6px;
        }

        .algorithm-badge {
          display: inline-block;
          padding: 5px 10px;
          border-radius: 20px;
          font-size: 11px;
          font-weight: 600;
          white-space: nowrap;
          background: #f1edff;
          color: #6d45c7;
        }

        .algorithm-fifo {
          background: #e9edff;
          color: #485fc7;
        }

        .algorithm-greedy {
          background: #e8f8ef;
          color: #16834a;
        }

        .algorithm-knapsack {
          background: #fff1e5;
          color: #bd671d;
        }

        .table-container h3 {
          margin-bottom: 16px;
        }
      `}</style>

    </div>
  );
}

export default Dashboard;
