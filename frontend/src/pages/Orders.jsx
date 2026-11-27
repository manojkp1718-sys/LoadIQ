
import { useEffect, useState } from "react";
import {
  getOrders,
  addOrder,
  deleteOrder,
  updateOrderStatus,
} from "../services/api";

function Orders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const initialForm = {
    customerName: "",
    destination: "",
    weight: "",
    volume: "",
    priority: "3",
    deadline: "",
  };

  const [form, setForm] = useState(initialForm);

  async function loadOrders() {
    try {
      setLoading(true);
      setError("");

      const data = await getOrders();
      setOrders(data);
    } catch (err) {
      setError(err.message || "Failed to load orders.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadOrders();
  }, []);

  function handleChange(e) {
    const { name, value } = e.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  }

  async function handleSubmit(e) {
    e.preventDefault();

    setMessage("");
    setError("");

    if (
      !form.customerName.trim() ||
      !form.destination.trim() ||
      Number(form.weight) <= 0 ||
      Number(form.volume) <= 0 ||
      !form.deadline
    ) {
      setError("Please enter valid order details.");
      return;
    }

    setSubmitting(true);

    try {
      await addOrder({
        customerName: form.customerName.trim(),
        destination: form.destination.trim(),
        weight: Number(form.weight),
        volume: Number(form.volume),
        priority: Number(form.priority),
        deadline: form.deadline.replace("T", " "),
      });

      setMessage("Order added successfully!");
      setForm(initialForm);

      await loadOrders();
    } catch (err) {
      setError(err.message || "Failed to add order.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id) {
    if (!window.confirm("Are you sure you want to delete this order?")) {
      return;
    }

    try {
      setError("");
      setMessage("");

      await deleteOrder(id);

      setMessage("Order deleted successfully!");
      await loadOrders();
    } catch (err) {
      setError(err.message || "Failed to delete order.");
    }
  }

  async function handleStatusChange(id, status) {
    try {
      setError("");
      setMessage("");

      await updateOrderStatus(id, status);

      setMessage("Order status updated successfully!");
      await loadOrders();
    } catch (err) {
      setError(err.message || "Failed to update order status.");
      await loadOrders();
    }
  }

  // Only allow valid lifecycle transitions.
  function getAvailableStatuses(status) {
    switch (String(status || "").toUpperCase()) {
      case "PENDING":
        return ["PENDING", "CANCELLED"];

      case "ASSIGNED":
        return ["ASSIGNED", "DELIVERED"];

      case "DELIVERED":
        return ["DELIVERED"];

      case "CANCELLED":
        return ["CANCELLED"];

      default:
        return [];
    }
  }

  function formatVolume(value) {
    if (value === null || value === undefined) {
      return "—";
    }

    return `${Number(value).toFixed(3)} m³`;
  }

  function formatWeight(value) {
    if (value === null || value === undefined) {
      return "—";
    }

    return `${Number(value).toFixed(2)} kg`;
  }

  const columns = [
    { key: "orderId", label: "Order ID" },
    { key: "customerName", label: "Customer" },
    { key: "destination", label: "Destination" },
    { key: "weight", label: "Weight" },
    { key: "volume", label: "Volume" },
    { key: "priority", label: "Priority" },
    { key: "deadline", label: "Deadline" },
    { key: "orderStatus", label: "Status" },
  ];

  function renderCell(order, key) {
    switch (key) {
      case "weight":
        return formatWeight(order.weight);

      case "volume":
        return formatVolume(order.volume);

      case "deadline":
        return order.deadline
          ? String(order.deadline).replace("T", " ")
          : "—";

      case "orderStatus":
        return (
          <span
            className={`order-status status-${String(
              order.orderStatus || ""
            ).toLowerCase()}`}
          >
            {order.orderStatus}
          </span>
        );

      default:
        return order[key] ?? "—";
    }
  }

  return (
    <div className="page-container orders-page">
      <div className="orders-heading">
        <div>
          <h2>Orders Management</h2>
          <p>
            Manage delivery orders, package dimensions and priority
            information in your LoadIQ database.
          </p>
        </div>

        <button
          type="button"
          className="secondary-button"
          onClick={loadOrders}
          disabled={loading}
        >
          {loading ? "Refreshing..." : "Refresh Orders"}
        </button>
      </div>

      {message && (
        <div className="success-message">
          {message}
        </div>
      )}

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {/* ADD NEW ORDER */}

      <div className="order-summary">
        <div className="section-heading">
          <div>
            <h3>Add New Order</h3>
            <p>
              Enter customer details and package capacity requirements.
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="order-form">
          <div className="order-form-field">
            <label htmlFor="customerName">Customer Name</label>
            <input
              id="customerName"
              name="customerName"
              type="text"
              placeholder="Enter customer name"
              value={form.customerName}
              onChange={handleChange}
              maxLength={150}
              required
            />
          </div>

          <div className="order-form-field">
            <label htmlFor="destination">Destination</label>
            <input
              id="destination"
              name="destination"
              type="text"
              placeholder="Enter delivery destination"
              value={form.destination}
              onChange={handleChange}
              maxLength={255}
              required
            />
          </div>

          <div className="order-form-field">
            <label htmlFor="weight">Weight (kg)</label>
            <input
              id="weight"
              name="weight"
              type="number"
              min="0.01"
              step="0.01"
              placeholder="e.g. 25.50"
              value={form.weight}
              onChange={handleChange}
              required
            />
          </div>

          <div className="order-form-field">
            <label htmlFor="volume">Volume (m³)</label>
            <input
              id="volume"
              name="volume"
              type="number"
              min="0.001"
              step="0.001"
              placeholder="e.g. 1.250"
              value={form.volume}
              onChange={handleChange}
              required
            />
          </div>

          <div className="order-form-field">
            <label htmlFor="priority">Priority Level</label>
            <select
              id="priority"
              name="priority"
              value={form.priority}
              onChange={handleChange}
              required
            >
              <option value="1">Priority 1 - Lowest</option>
              <option value="2">Priority 2 - Low</option>
              <option value="3">Priority 3 - Medium</option>
              <option value="4">Priority 4 - High</option>
              <option value="5">Priority 5 - Highest</option>
            </select>
          </div>

          <div className="order-form-field">
            <label htmlFor="deadline">Delivery Deadline</label>
            <input
              id="deadline"
              name="deadline"
              type="datetime-local"
              value={form.deadline}
              onChange={handleChange}
              required
            />
          </div>

          <div className="order-form-actions">
            <button
              type="submit"
              className="primary-button"
              disabled={submitting}
            >
              {submitting ? "Adding Order..." : "Add Order"}
            </button>
          </div>
        </form>
      </div>

      {/* ALL ORDERS */}

      <div className="table-container orders-table-container">
        <div className="section-heading table-heading">
          <div>
            <h3>All Orders</h3>
            <p>
              View and manage all delivery orders.
            </p>
          </div>

          <span className="order-count">
            {orders.length} Orders
          </span>
        </div>

        {loading ? (
          <div className="orders-empty-state">
            Loading orders...
          </div>
        ) : orders.length === 0 ? (
          <div className="orders-empty-state">
            No orders found. Add your first delivery order above.
          </div>
        ) : (
          <div className="orders-table-scroll">
            <table className="orders-table">
              <thead>
                <tr>
                  {columns.map((column) => (
                    <th key={column.key}>{column.label}</th>
                  ))}
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {orders.map((order) => {
                  const availableStatuses =
                    getAvailableStatuses(order.orderStatus);

                  const isTerminal =
                    ["DELIVERED", "CANCELLED"].includes(
                      String(order.orderStatus || "").toUpperCase()
                    );

                  return (
                    <tr key={order.orderId}>
                      {columns.map((column) => (
                        <td key={column.key}>
                          {renderCell(order, column.key)}
                        </td>
                      ))}

                      <td>
                        <div className="order-actions">
                          <select
                            className="status-select"
                            aria-label={`Update status for order ${order.orderId}`}
                            value={order.orderStatus}
                            disabled={isTerminal}
                            onChange={(e) =>
                              handleStatusChange(
                                order.orderId,
                                e.target.value
                              )
                            }
                          >
                            {availableStatuses.map((status) => (
                              <option key={status} value={status}>
                                {status}
                              </option>
                            ))}
                          </select>

                          <button
                            type="button"
                            className="delete-button"
                            onClick={() => handleDelete(order.orderId)}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default Orders;
