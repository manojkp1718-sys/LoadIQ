
import { useEffect, useState } from "react";
import { getOptimizationHistory } from "../services/api";

function OptimizationHistory() {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadHistory() {
    try {
      setLoading(true);
      setError("");

      const data = await getOptimizationHistory();
      setRecords(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Unable to load optimization history.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadHistory();
  }, []);

  // Format date and time.
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
      second: "2-digit",
      hour12: true,
    });
  }

  // Format algorithm name.
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

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Optimization History</h2>
          <p>
            Review previously saved optimization runs and their performance.
          </p>
        </div>

        <div className="history-count">
          Total Runs: <strong>{records.length}</strong>
        </div>
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {loading ? (
        <div className="history-state">
          <p>Loading optimization history...</p>
        </div>
      ) : records.length === 0 ? (
        <div className="history-state">
          <h3>No Optimization History</h3>
          <p>
            Saved optimization runs will appear here.
          </p>
        </div>
      ) : (
        <div className="table-container">
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
              {records.map((record) => (
                <tr key={record.run_id}>
                  <td>{record.run_id}</td>

                  <td>
                    <span className={getAlgorithmClass(record.algorithm_name)}>
                      {record.algorithm_name || "UNKNOWN"}
                    </span>
                  </td>

                  <td>{record.total_orders}</td>

                  <td>{record.assigned_orders}</td>

                  <td>
                    {Number(record.total_weight || 0).toFixed(2)} kg
                  </td>

                  <td>
                    {record.total_volume != null
                      ? `${Number(record.total_volume).toFixed(3)} m³`
                      : "—"}
                  </td>

                  <td>
                    {Number(record.total_priority_score || 0).toFixed(2)}
                  </td>

                  <td>
                    {Number(record.execution_time_ms || 0).toFixed(3)} ms
                  </td>

                  <td>
                    {formatDateTime(record.run_date)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <button
        onClick={loadHistory}
        disabled={loading}
        className="refresh-button"
      >
        {loading ? "Refreshing..." : "Refresh History"}
      </button>

      <style>{`
        .page-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          gap: 16px;
          flex-wrap: wrap;
          margin-bottom: 20px;
        }

        .page-header p {
          margin-top: 6px;
          color: #8b83a5;
          font-size: 13px;
        }

        .history-count {
          background: #f1edff;
          color: #6d45c7;
          padding: 10px 16px;
          border-radius: 10px;
          font-size: 13px;
        }

        .history-state {
          padding: 35px;
          text-align: center;
          background: #ffffff;
          border-radius: 12px;
          margin-bottom: 18px;
        }

        .history-state p {
          color: #8b83a5;
          margin-top: 8px;
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

        .refresh-button {
          margin-top: 16px;
          cursor: pointer;
        }

        .refresh-button:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
      `}</style>
    </div>
  );
}

export default OptimizationHistory;
