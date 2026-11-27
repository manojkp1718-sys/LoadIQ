
import { useEffect, useState } from "react";
import {
  getVehicles,
  addVehicle,
  deleteVehicle,
  updateVehicleAvailability,
} from "../services/api";
import "./Vehicles.css";

function Vehicles() {
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [deletingId, setDeletingId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [updatingId, setUpdatingId] = useState(null);

  const initialForm = {
    registrationNumber: "",
    vehicleType: "",
    capacity: "",
    volumeCapacity: "",
  };

  const [form, setForm] = useState(initialForm);

  async function loadVehicles() {
    try {
      setLoading(true);
      setError("");

      const data = await getVehicles();
      setVehicles(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Unable to load vehicles.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadVehicles();
  }, []);

  function handleChange(e) {
    const { name, value } = e.target;

    setForm((current) => ({
      ...current,
      [name]: value,
    }));
  }

  async function handleSubmit(e) {
    e.preventDefault();

    setError("");
    setMessage("");

    try {
      setSaving(true);

      await addVehicle({
        registrationNumber: form.registrationNumber.trim().toUpperCase(),
        vehicleType: form.vehicleType.trim(),
        capacity: Number(form.capacity),
        volumeCapacity: Number(form.volumeCapacity),
      });

      setMessage("Vehicle added successfully.");
      setForm(initialForm);

      await loadVehicles();
    } catch (err) {
      setError(err.message || "Unable to add vehicle.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id) {
    if (!window.confirm("Are you sure you want to delete this vehicle?")) {
      return;
    }

    try {
      setDeletingId(id);
      setError("");
      setMessage("");

      await deleteVehicle(id);

      setMessage("Vehicle deleted successfully.");
      await loadVehicles();
    } catch (err) {
      setError(err.message || "Unable to delete vehicle.");
    } finally {
      setDeletingId(null);
    }
  }

  async function handleAvailabilityChange(id, availability) {
    try {
      setUpdatingId(id);
      setError("");
      setMessage("");

      await updateVehicleAvailability(id, availability);

      setMessage("Vehicle availability updated.");
      await loadVehicles();
    } catch (err) {
      setError(err.message || "Unable to update vehicle availability.");
      await loadVehicles();
    } finally {
      setUpdatingId(null);
    }
  }

  const availableCount = vehicles.filter(
    (vehicle) => vehicle.availability === "AVAILABLE"
  ).length;

  const inUseCount = vehicles.filter(
    (vehicle) => vehicle.availability === "IN_USE"
  ).length;

  const maintenanceCount = vehicles.filter(
    (vehicle) => vehicle.availability === "MAINTENANCE"
  ).length;

  const totalWeightCapacity = vehicles.reduce(
    (total, vehicle) => total + Number(vehicle.capacity || 0),
    0
  );

  const totalVolumeCapacity = vehicles.reduce(
    (total, vehicle) => total + Number(vehicle.volumeCapacity || 0),
    0
  );

  function formatWeight(value) {
    return `${Number(value || 0).toLocaleString("en-IN", {
      maximumFractionDigits: 2,
    })} kg`;
  }

  function formatVolume(value) {
    return `${Number(value || 0).toFixed(3)} m³`;
  }

  return (
    <div className="page-container vehicles-page">
      <div className="vehicles-page-heading">
        <div>
          <div className="vehicles-eyebrow">FLEET MANAGEMENT</div>
          <h2>Vehicles</h2>
          <p>
            Manage your delivery fleet, weight capacity, volume capacity
            and vehicle availability.
          </p>
        </div>

        <div className="vehicles-heading-count">
          <span className="vehicles-live-dot" />
          {vehicles.length} vehicles registered
        </div>
      </div>

      {message && (
        <div className="vehicles-alert vehicles-alert-success">
          {message}
        </div>
      )}

      {error && (
        <div className="vehicles-alert vehicles-alert-error">
          {error}
        </div>
      )}

      <section className="vehicles-stat-grid" aria-label="Vehicle summary">
        <article className="vehicles-stat-card">
          <div className="vehicles-stat-icon vehicles-icon-purple">▰</div>
          <div>
            <span>Total vehicles</span>
            <strong>{vehicles.length}</strong>
            <small>Registered fleet</small>
          </div>
        </article>

        <article className="vehicles-stat-card">
          <div className="vehicles-stat-icon vehicles-icon-green">✓</div>
          <div>
            <span>Available</span>
            <strong>{availableCount}</strong>
            <small>Ready for assignment</small>
          </div>
        </article>

        <article className="vehicles-stat-card">
          <div className="vehicles-stat-icon vehicles-icon-blue">↗</div>
          <div>
            <span>In use</span>
            <strong>{inUseCount}</strong>
            <small>Currently assigned</small>
          </div>
        </article>

        <article className="vehicles-stat-card">
          <div className="vehicles-stat-icon vehicles-icon-amber">⚙</div>
          <div>
            <span>Total fleet capacity</span>
            <strong>
              {totalWeightCapacity.toLocaleString("en-IN", {
                maximumFractionDigits: 2,
              })}{" "}
              <small>kg</small>
            </strong>
            <small>
              {totalVolumeCapacity.toFixed(3)} m³ total volume
            </small>
            <small>{maintenanceCount} in maintenance</small>
          </div>
        </article>
      </section>

      <section className="vehicles-panel">
        <div className="vehicles-panel-heading">
          <div>
            <h3>Add a vehicle</h3>
            <p>
              Register a vehicle with both weight and volume capacity
              for delivery optimization.
            </p>
          </div>

          <span className="vehicles-panel-step">NEW FLEET ENTRY</span>
        </div>

        <form onSubmit={handleSubmit} className="vehicles-form">
          <label>
            <span>Registration number</span>
            <input
              name="registrationNumber"
              placeholder="e.g. KA01AB1234"
              value={form.registrationNumber}
              onChange={handleChange}
              maxLength={30}
              required
            />
          </label>

          <label>
            <span>Vehicle type</span>
            <input
              name="vehicleType"
              placeholder="e.g. Truck, Mini Truck, Van"
              value={form.vehicleType}
              onChange={handleChange}
              maxLength={80}
              required
            />
          </label>

          <label>
            <span>Weight capacity (kg)</span>
            <input
              name="capacity"
              type="number"
              min="0.01"
              step="0.01"
              placeholder="e.g. 1000"
              value={form.capacity}
              onChange={handleChange}
              required
            />
          </label>

          <label>
            <span>Volume capacity (m³)</span>
            <input
              name="volumeCapacity"
              type="number"
              min="0.001"
              step="0.001"
              placeholder="e.g. 15.500"
              value={form.volumeCapacity}
              onChange={handleChange}
              required
            />
          </label>

          <button
            className="vehicles-add-button"
            type="submit"
            disabled={saving}
          >
            <span aria-hidden="true">＋</span>
            {saving ? "Adding..." : "Add vehicle"}
          </button>
        </form>
      </section>

      <section className="vehicles-panel vehicles-table-panel">
        <div className="vehicles-panel-heading">
          <div>
            <h3>All vehicles</h3>
            <p>
              View fleet capacities, update availability or remove a
              vehicle from your records.
            </p>
          </div>

          <span className="vehicles-record-count">
            {vehicles.length} records
          </span>
        </div>

        {loading ? (
          <div className="vehicles-empty-state">
            <span className="vehicles-loader" />
            <p>Loading your fleet...</p>
          </div>
        ) : vehicles.length === 0 ? (
          <div className="vehicles-empty-state">
            <div className="vehicles-empty-icon">▰</div>
            <strong>No vehicles registered yet</strong>
            <p>Add your first delivery vehicle using the form above.</p>
          </div>
        ) : (
          <div className="vehicles-table-scroll">
            <table className="vehicles-table">
              <thead>
                <tr>
                  <th>Vehicle</th>
                  <th>Registration number</th>
                  <th>Vehicle type</th>
                  <th>Weight capacity</th>
                  <th>Volume capacity</th>
                  <th>Availability</th>
                  <th className="vehicles-actions-heading">Actions</th>
                </tr>
              </thead>

              <tbody>
                {vehicles.map((vehicle) => (
                  <tr key={vehicle.vehicleId}>
                    <td>
                      <span className="vehicles-id">
                        VH-{String(vehicle.vehicleId).padStart(3, "0")}
                      </span>
                    </td>

                    <td className="vehicles-registration">
                      {vehicle.registrationNumber}
                    </td>

                    <td>{vehicle.vehicleType}</td>

                    <td>
                      <strong className="vehicles-capacity">
                        {formatWeight(vehicle.capacity)}
                      </strong>
                    </td>

                    <td>
                      <strong className="vehicles-capacity">
                        {formatVolume(vehicle.volumeCapacity)}
                      </strong>
                    </td>

                    <td>
                      <select
                        className={`vehicles-status-select status-${String(
                          vehicle.availability || ""
                        ).toLowerCase()}`}
                        value={vehicle.availability}
                        disabled={
                          updatingId === vehicle.vehicleId ||
                          vehicle.availability === "IN_USE"
                        }
                        onChange={(e) =>
                          handleAvailabilityChange(
                            vehicle.vehicleId,
                            e.target.value
                          )
                        }
                        aria-label={`Availability for ${vehicle.registrationNumber}`}
                        title={
                          vehicle.availability === "IN_USE"
                            ? "Vehicle availability is controlled by assignments."
                            : "Change vehicle availability"
                        }
                      >
                        <option value="AVAILABLE">Available</option>
                        <option value="IN_USE" disabled>
                          In use
                        </option>
                        <option value="MAINTENANCE">Maintenance</option>
                      </select>
                    </td>

                    <td className="vehicles-actions-cell">
                      <button
                        className="vehicles-delete-button"
                        type="button"
                        onClick={() => handleDelete(vehicle.vehicleId)}
                        disabled={deletingId === vehicle.vehicleId}
                      >
                        {deletingId === vehicle.vehicleId
                          ? "Deleting..."
                          : "Delete"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

export default Vehicles;
