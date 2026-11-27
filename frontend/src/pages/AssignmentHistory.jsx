
import { useEffect, useState } from "react";
import { getAssignmentHistory } from "../services/api";

function AssignmentHistory() {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadHistory() {
    try {
      setLoading(true);
      setError("");

      const data = await getAssignmentHistory();
      setRecords(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Unable to load assignment history.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadHistory();
  }, []);

  // Format timestamp into readable local date and time.
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

  function getStatusClass(status) {
    switch (status?.toUpperCase()) {
      case "ASSIGNED":
        return "status-badge status-assigned";

      case "COMPLETED":
        return "status-badge status-completed";

      case "CANCELLED":
        return "status-badge status-cancelled";

      default:
        return "status-badge";
    }
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Assignment History</h2>
          <p>View and monitor delivery assignments recorded in MySQL.</p>
        </div>

        <div className="history-count">
          Total Records: <strong>{records.length}</strong>
        </div>
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {loading ? (
        <div className="history-state">
          <p>Loading assignment history...</p>
        </div>
      ) : records.length === 0 ? (
        <div className="history-state">
          <h3>No Assignment Records</h3>
          <p>Assignment records will appear here after delivery loads are saved.</p>
        </div>
      ) : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Assignment ID</th>
                <th>Order ID</th>
                <th>Customer</th>
                <th>Destination</th>
                <th>Vehicle ID</th>
                <th>Registration</th>
                <th>Weight (kg)</th>
                <th>Assigned At</th>
                <th>Status</th>
              </tr>
            </thead>

            <tbody>
              {records.map((record) => (
                <tr key={record.assignment_id}>
                  <td>{record.assignment_id}</td>
                  <td>{record.order_id}</td>
                  <td>{record.customer_name || "—"}</td>
                  <td>{record.destination || "—"}</td>
                  <td>{record.vehicle_id}</td>
                  <td>{record.registration_number || "—"}</td>

                  <td>
                    {Number(record.assigned_weight || 0).toFixed(2)}
                  </td>

                  <td>
                    {formatDateTime(record.assigned_at)}
                  </td>

                  <td>
                    <span className={getStatusClass(record.assignment_status)}>
                      {record.assignment_status || "UNKNOWN"}
                    </span>
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

        .status-badge {
          display: inline-block;
          padding: 5px 10px;
          border-radius: 20px;
          font-size: 11px;
          font-weight: 600;
          white-space: nowrap;
        }

        .status-assigned {
          background: #e8f8ef;
          color: #16834a;
        }

        .status-completed {
          background: #e9edff;
          color: #485fc7;
        }

        .status-cancelled {
          background: #fff0f0;
          color: #d14343;
        }

        .refresh-button {
          margin-top: 16px;
          cursor: pointer;
        }

        .refresh-button:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        .error-message {
          margin-bottom: 16px;
        }
      `}</style>
    </div>
  );
}

export default AssignmentHistory;
