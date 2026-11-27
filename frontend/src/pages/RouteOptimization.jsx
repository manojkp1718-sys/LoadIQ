
import { useEffect, useState } from "react";

import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  Polyline,
  useMap,
} from "react-leaflet";

import L from "leaflet";
import "leaflet/dist/leaflet.css";

import {
  getOrders,
  findMultiStopRoute,
} from "../services/api";

// Fallback coordinates for exact city-name destinations only.
// Neighborhood addresses must use their actual stored coordinates.
const locations = {
  Bengaluru: [12.9716, 77.5946],
  Mysuru: [12.2958, 76.6394],
  Chennai: [13.0827, 80.2707],
  Hyderabad: [17.3850, 78.4867],
  Mumbai: [19.0760, 72.8777],
};

function getOrderCoordinates(order) {
  const latitude = Number(
    order.destinationLatitude ??
    order.destination_latitude ??
    order.latitude
  );

  const longitude = Number(
    order.destinationLongitude ??
    order.destination_longitude ??
    order.longitude
  );

  if (
    Number.isFinite(latitude) &&
    Number.isFinite(longitude) &&
    latitude >= -90 &&
    latitude <= 90 &&
    longitude >= -180 &&
    longitude <= 180 &&
    !(latitude === 0 && longitude === 0)
  ) {
    return [latitude, longitude];
  }

  // Only use fallback coordinates for exact city-name matches.
  const destination = String(order.destination || "").trim();

  const city = Object.keys(locations).find(
    (name) => name.toLowerCase() === destination.toLowerCase()
  );

  return city ? locations[city] : null;
}

function createNumberIcon(number, isStart = false) {
  const background = isStart ? "#16a34a" : "#7c3aed";

  return L.divIcon({
    className: "",
    html: `
      <div style="
        background:${background};
        color:white;
        width:30px;
        height:30px;
        border:3px solid white;
        border-radius:50%;
        display:flex;
        align-items:center;
        justify-content:center;
        font-weight:bold;
        font-size:13px;
        box-shadow:0 2px 8px #0005;
      ">
        ${number}
      </div>
    `,
    iconSize: [36, 36],
    iconAnchor: [18, 18],
  });
}

function FitRoute({ positions }) {
  const map = useMap();

  useEffect(() => {
    if (positions?.length > 0) {
      map.fitBounds(positions, {
        padding: [40, 40],
      });
    }
  }, [map, positions]);

  return null;
}

function RouteOptimization() {
  const [orders, setOrders] = useState([]);
  const [start, setStart] = useState("Bengaluru");
  const [selectedOrderIds, setSelectedOrderIds] = useState([]);
  const [result, setResult] = useState(null);

  const [loadingOrders, setLoadingOrders] = useState(true);
  const [loadingRoute, setLoadingRoute] = useState(false);

  const [error, setError] = useState("");

  useEffect(() => {
    async function loadOrders() {
      try {
        setLoadingOrders(true);
        setError("");

        const data = await getOrders();

        if (!Array.isArray(data)) {
          throw new Error(
            "Unexpected orders response from backend."
          );
        }

        setOrders(data);
      } catch (err) {
        setError(
          err.message || "Unable to load delivery orders."
        );
      } finally {
        setLoadingOrders(false);
      }
    }

    loadOrders();
  }, []);

  // Only pending orders can be selected.
  const pendingOrders = orders.filter(
    (order) =>
      String(order.orderStatus || "").toUpperCase() === "PENDING"
  );

  // Use stored coordinates or an exact city fallback.
  const supportedOrders = pendingOrders.filter(
    (order) => getOrderCoordinates(order) !== null
  );

  const unsupportedOrders = pendingOrders.filter(
    (order) => getOrderCoordinates(order) === null
  );

  function toggleOrder(orderId) {
    setSelectedOrderIds((previous) =>
      previous.some((id) => String(id) === String(orderId))
        ? previous.filter(
            (id) => String(id) !== String(orderId)
          )
        : [...previous, orderId]
    );

    setResult(null);
    setError("");
  }

  function selectAllOrders() {
    setSelectedOrderIds(
      supportedOrders.map((order) => order.orderId)
    );

    setResult(null);
    setError("");
  }

  function clearSelection() {
    setSelectedOrderIds([]);
    setResult(null);
    setError("");
  }

  function handleStartChange(value) {
    setStart(value);
    setResult(null);
    setError("");
  }

  async function handleOptimize(e) {
    e.preventDefault();

    setError("");
    setResult(null);

    const selectedOrders = supportedOrders.filter((order) =>
      selectedOrderIds.some(
        (id) => String(id) === String(order.orderId)
      )
    );

    if (selectedOrders.length === 0) {
      setError(
        "Please select at least one pending delivery order with valid coordinates."
      );
      return;
    }

    // Group orders by destination and exact coordinates.
    const destinationMap = new Map();

    selectedOrders.forEach((order) => {
      const coordinates = getOrderCoordinates(order);

      if (!coordinates) return;

      const destination = String(
        order.destination || "Delivery location"
      ).trim();

      const key = `${destination.toLowerCase()}_${coordinates[0]}_${coordinates[1]}`;

      if (!destinationMap.has(key)) {
        destinationMap.set(key, {
          name: destination,
          latitude: coordinates[0],
          longitude: coordinates[1],
          orders: [],
        });
      }

      destinationMap.get(key).orders.push(order);
    });

    const destinations = [...destinationMap.values()];

    const [startLatitude, startLongitude] = locations[start];

    const sameAsStart = destinations.some(
      (destination) =>
        Math.abs(destination.latitude - startLatitude) < 0.000001 &&
        Math.abs(destination.longitude - startLongitude) < 0.000001
    );

    if (sameAsStart) {
      setError(
        "A selected delivery destination matches the starting location. Change the starting location or selection."
      );
      return;
    }

    if (destinations.length > 9) {
      setError(
        "A maximum of 9 unique delivery destinations is supported."
      );
      return;
    }

    const stops = destinations.map((destination) => ({
      name: destination.name,
      latitude: destination.latitude,
      longitude: destination.longitude,
    }));

    const request = {
      startName: start,
      startLatitude,
      startLongitude,
      stops,
      selectedOrderIds: selectedOrders.map((order) =>
        Number(order.orderId)
      ),
    };

    try {
      setLoadingRoute(true);

      const data = await findMultiStopRoute(request);

      if (!data.found) {
        throw new Error(
          data.message || "No optimized route found."
        );
      }

      if (!data.geometry?.coordinates?.length) {
        throw new Error(
          "The routing service returned no route geometry."
        );
      }

      // GeoJSON coordinates are [longitude, latitude].
      // Leaflet coordinates are [latitude, longitude].
      const positions = data.geometry.coordinates.map(
        ([longitude, latitude]) => [
          latitude,
          longitude,
        ]
      );

      setResult({
        ...data,
        positions,
        selectedOrders,
        destinationMap,
      });
    } catch (err) {
      setError(
        err.message || "Unable to optimize the delivery route."
      );
    } finally {
      setLoadingRoute(false);
    }
  }

  return (
    <div className="page-container">
      <h2>Intelligent Route Optimization</h2>

      <p>
        Select pending delivery orders and automatically
        optimize the delivery sequence using real-road routing.
      </p>

      <div className="algorithm-panel">
        <h3>1. Select Starting Location</h3>

        <select
          value={start}
          onChange={(e) => handleStartChange(e.target.value)}
        >
          {Object.keys(locations).map((city) => (
            <option key={city} value={city}>
              {city}
            </option>
          ))}
        </select>

        <h3 style={{ marginTop: "25px" }}>
          2. Select Pending Delivery Orders
        </h3>

        {loadingOrders ? (
          <p>Loading orders from MySQL...</p>
        ) : pendingOrders.length === 0 ? (
          <p>No pending delivery orders found.</p>
        ) : (
          <>
            <p>
              Available pending orders:{" "}
              <strong>{pendingOrders.length}</strong>
            </p>

            <div style={{ overflowX: "auto" }}>
              <table
                style={{
                  width: "100%",
                  borderCollapse: "collapse",
                }}
              >
                <thead>
                  <tr>
                    <th style={{ textAlign: "left", padding: "10px" }}>
                      Select
                    </th>
                    <th style={{ textAlign: "left", padding: "10px" }}>
                      Order ID
                    </th>
                    <th style={{ textAlign: "left", padding: "10px" }}>
                      Customer
                    </th>
                    <th style={{ textAlign: "left", padding: "10px" }}>
                      Destination
                    </th>
                    <th style={{ textAlign: "left", padding: "10px" }}>
                      Priority
                    </th>
                    <th style={{ textAlign: "left", padding: "10px" }}>
                      Weight
                    </th>
                    <th style={{ textAlign: "left", padding: "10px" }}>
                      Coordinates
                    </th>
                  </tr>
                </thead>

                <tbody>
                  {pendingOrders.map((order) => {
                    const coordinates = getOrderCoordinates(order);

                    const supported = coordinates !== null;

                    const checked = selectedOrderIds.some(
                      (id) =>
                        String(id) === String(order.orderId)
                    );

                    return (
                      <tr key={order.orderId}>
                        <td style={{ padding: "10px" }}>
                          <input
                            type="checkbox"
                            checked={checked}
                            disabled={!supported}
                            onChange={() =>
                              toggleOrder(order.orderId)
                            }
                          />
                        </td>

                        <td style={{ padding: "10px" }}>
                          {order.orderId}
                        </td>

                        <td style={{ padding: "10px" }}>
                          {order.customerName}
                        </td>

                        <td style={{ padding: "10px" }}>
                          {order.destination}
                        </td>

                        <td style={{ padding: "10px" }}>
                          {order.priority}
                        </td>

                        <td style={{ padding: "10px" }}>
                          {order.weight} kg
                        </td>

                        <td style={{ padding: "10px" }}>
                          {supported ? (
                            <span style={{ color: "#16a34a" }}>
                              Available
                            </span>
                          ) : (
                            <span style={{ color: "#dc2626" }}>
                              Unavailable
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </>
        )}

        <div
          className="algorithm-actions"
          style={{
            marginTop: "20px",
            display: "flex",
            gap: "10px",
            flexWrap: "wrap",
          }}
        >
          <button
            type="button"
            onClick={selectAllOrders}
            disabled={supportedOrders.length === 0}
          >
            Select All Supported Orders
          </button>

          <button
            type="button"
            onClick={clearSelection}
          >
            Clear Selection
          </button>

          <button
            type="button"
            onClick={handleOptimize}
            disabled={
              loadingRoute ||
              loadingOrders ||
              selectedOrderIds.length === 0
            }
          >
            {loadingRoute
              ? "Optimizing Route..."
              : "Optimize Selected Orders"}
          </button>
        </div>

        <p>
          <strong>Selected orders:</strong>{" "}
          {selectedOrderIds.length}
        </p>

        {unsupportedOrders.length > 0 && (
          <p style={{ color: "#d97706" }}>
            {unsupportedOrders.length} pending order(s) do not
            have valid stored coordinates. Their destinations
            must be geocoded before routing.
          </p>
        )}
      </div>

      {error && (
        <p className="error-message">{error}</p>
      )}

      {result && (
        <>
          <div className="result-panel">
            <h3>3. Optimized Route Results</h3>

            <div className="dashboard-grid">
              <div className="dashboard-card">
                <h3>Total Road Distance</h3>
                <h2>
                  {Number(result.distanceKm).toFixed(2)} km
                </h2>
              </div>

              <div className="dashboard-card">
                <h3>Estimated Travel Time</h3>
                <h2>
                  {Number(result.durationMinutes).toFixed(0)} min
                </h2>
              </div>

              <div className="dashboard-card">
                <h3>Selected Orders</h3>
                <h2>{result.selectedOrders.length}</h2>
              </div>

              <div className="dashboard-card">
                <h3>Delivery Stops</h3>
                <h2>
                  {result.optimizedStops.length - 1}
                </h2>
              </div>
            </div>

            <div style={{ marginTop: "20px" }}>
              <h3>Saved Route Information</h3>

              <p>
                <strong>Route ID:</strong>{" "}
                {result.routeId ?? "Not returned"}
              </p>

              <p>
                <strong>Routing Engine:</strong> OSRM
              </p>
            </div>

            <h3 style={{ marginTop: "20px" }}>
              Optimized Delivery Sequence
            </h3>

            {result.optimizedStops.map((stop) => {
              const matchingDestination = [
                ...result.destinationMap.values(),
              ].find(
                (destination) =>
                  destination.name === stop.name
              );

              return (
                <div
                  key={stop.sequence}
                  style={{ marginBottom: "15px" }}
                >
                  <p>
                    <strong>
                      {stop.sequence}. {stop.name}
                    </strong>

                    {stop.sequence === 1
                      ? " (Starting Point)"
                      : ""}
                  </p>

                  {matchingDestination?.orders.map((order) => (
                    <p
                      key={order.orderId}
                      style={{ marginLeft: "20px" }}
                    >
                      Order #{order.orderId} –{" "}
                      {order.customerName}
                    </p>
                  ))}
                </div>
              );
            })}

            <p className="success-message">
              {result.message}
            </p>
          </div>

          <div className="algorithm-panel">
            <h3>4. Optimized Delivery Route Map</h3>

            <div
              style={{
                height: "450px",
                width: "100%",
                borderRadius: "12px",
                overflow: "hidden",
                marginTop: "15px",
                zIndex: 1,
              }}
            >
              <MapContainer
                center={result.positions[0]}
                zoom={7}
                scrollWheelZoom={true}
                style={{
                  height: "100%",
                  width: "100%",
                }}
              >
                <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {result.optimizedStops.map((stop) => {
                  const matchingDestination = [
                    ...result.destinationMap.values(),
                  ].find(
                    (destination) =>
                      destination.name === stop.name
                  );

                  return (
                    <Marker
                      key={stop.sequence}
                      position={[
                        stop.latitude,
                        stop.longitude,
                      ]}
                      icon={createNumberIcon(
                        stop.sequence,
                        stop.sequence === 1
                      )}
                    >
                      <Popup>
                        <strong>
                          Stop {stop.sequence}: {stop.name}
                        </strong>

                        {matchingDestination?.orders.map((order) => (
                          <p key={order.orderId}>
                            Order #{order.orderId} –{" "}
                            {order.customerName}
                          </p>
                        ))}
                      </Popup>
                    </Marker>
                  );
                })}

                <Polyline
                  positions={result.positions}
                  pathOptions={{
                    color: "#7c3aed",
                    weight: 5,
                    opacity: 0.9,
                  }}
                />

                <FitRoute positions={result.positions} />
              </MapContainer>
            </div>

            <div
              style={{
                display: "flex",
                gap: "20px",
                flexWrap: "wrap",
                marginTop: "15px",
              }}
            >
              <span>
                <strong style={{ color: "#16a34a" }}>●</strong>{" "}
                Starting Point
              </span>

              <span>
                <strong style={{ color: "#7c3aed" }}>●</strong>{" "}
                Delivery Stops
              </span>

              <span>
                <strong style={{ color: "#7c3aed" }}>━</strong>{" "}
                Road Route
              </span>
            </div>
          </div>
        </>
      )}

      <div className="algorithm-panel">
        <h3>Routing Technology</h3>

        <p><strong>Database:</strong> MySQL</p>
        <p><strong>Backend:</strong> Java Spring Boot</p>
        <p><strong>Routing Engine:</strong> OSRM Trip API</p>
        <p><strong>Map Provider:</strong> OpenStreetMap</p>
        <p><strong>Frontend:</strong> React Leaflet</p>
        <p><strong>Optimization:</strong> Multi-stop route sequencing</p>
      </div>
    </div>
  );
}

export default RouteOptimization;
