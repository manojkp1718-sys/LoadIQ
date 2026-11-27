
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
  getRouteHistory,
  getRouteById,
} from "../services/api";

// Read either camelCase or snake_case response fields.
function readField(object, ...names) {
  for (const name of names) {
    if (object?.[name] !== undefined && object?.[name] !== null) {
      return object[name];
    }
  }

  return null;
}

// Parse JSON values returned by MySQL or the backend.
function parseJson(value, fallback = []) {
  if (value === null || value === undefined || value === "") {
    return fallback;
  }

  if (typeof value === "object") {
    return value;
  }

  try {
    return JSON.parse(value);
  } catch {
    return fallback;
  }
}

// Convert a coordinate into a Leaflet [latitude, longitude] pair.
function getPosition(item) {
  const rawLatitude = readField(
    item,
    "latitude",
    "lat",
    "startLatitude",
    "start_latitude"
  );

  const rawLongitude = readField(
    item,
    "longitude",
    "lon",
    "lng",
    "startLongitude",
    "start_longitude"
  );

  if (
    rawLatitude === null ||
    rawLongitude === null ||
    rawLatitude === "" ||
    rawLongitude === ""
  ) {
    return null;
  }

  const latitude = Number(rawLatitude);
  const longitude = Number(rawLongitude);

  if (
    !Number.isFinite(latitude) ||
    !Number.isFinite(longitude) ||
    latitude < -90 ||
    latitude > 90 ||
    longitude < -180 ||
    longitude > 180
  ) {
    return null;
  }

  return [latitude, longitude];
}

// Create numbered map markers.
function createMarker(number) {
  return L.divIcon({
    className: "loadiq-route-marker",
    html: `<div style="
      background:#7c3aed;
      color:white;
      width:30px;
      height:30px;
      border-radius:50%;
      display:flex;
      align-items:center;
      justify-content:center;
      font-weight:bold;
      border:2px solid white;
      box-shadow:0 2px 6px rgba(0,0,0,0.35);
    ">${number}</div>`,
    iconSize: [30, 30],
    iconAnchor: [15, 15],
  });
}

// Adjust the map to display the entire route.
function FitRoute({ positions }) {
  const map = useMap();

  useEffect(() => {
    if (positions.length > 0) {
      map.fitBounds(positions, { padding: [35, 35] });
    }
  }, [map, positions]);

  return null;
}

function RouteHistory() {
  const [routes, setRoutes] = useState([]);
  const [selectedRoute, setSelectedRoute] = useState(null);
  const [loading, setLoading] = useState(true);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [error, setError] = useState("");

  // Load all saved routes.
  useEffect(() => {
    loadRoutes();
  }, []);

  async function loadRoutes() {
    try {
      setLoading(true);
      setError("");

      const response = await getRouteHistory();

      const routeList = Array.isArray(response)
        ? response
        : response?.routes || response?.data || [];

      setRoutes(Array.isArray(routeList) ? routeList : []);
    } catch (err) {
      setError(err.message || "Unable to load route history.");
    } finally {
      setLoading(false);
    }
  }

  // Load details of one selected route.
  async function openRoute(route) {
    const routeId = readField(route, "routeId", "route_id", "id");

    if (!routeId) {
      setError("Route ID is missing from this record.");
      return;
    }

    try {
      setDetailsLoading(true);
      setError("");

      const details = await getRouteById(routeId);

      setSelectedRoute(details);
    } catch (err) {
      setError(err.message || "Unable to load route details.");
    } finally {
      setDetailsLoading(false);
    }
  }

  const route = selectedRoute;

  const parsedStops = route
    ? parseJson(
        readField(route, "optimizedStops", "optimized_stops"),
        []
      )
    : [];

  const optimizedStops = Array.isArray(parsedStops)
    ? parsedStops
    : [];

  const geometry = route
    ? parseJson(
        readField(route, "routeGeometry", "route_geometry"),
        null
      )
    : null;

  const coordinates = Array.isArray(geometry?.coordinates)
    ? geometry.coordinates
    : [];

  // GeoJSON coordinates use [longitude, latitude].
  const routePositions = coordinates
    .filter(
      (coordinate) =>
        Array.isArray(coordinate) &&
        coordinate.length >= 2 &&
        Number.isFinite(Number(coordinate[0])) &&
        Number.isFinite(Number(coordinate[1])) &&
        coordinate[0] !== null &&
        coordinate[1] !== null
    )
    .map((coordinate) => [
      Number(coordinate[1]),
      Number(coordinate[0]),
    ])
    .filter(
      ([latitude, longitude]) =>
        latitude >= -90 &&
        latitude <= 90 &&
        longitude >= -180 &&
        longitude <= 180
    );

  // Use the route geometry for the line and stop coordinates for markers.
  const stopPositions = optimizedStops
    .map((stop) => ({
      stop,
      position: getPosition(stop),
    }))
    .filter((item) => item.position !== null);

  const mapPositions =
    routePositions.length > 0
      ? routePositions
      : stopPositions.map((item) => item.position);

  const startLocation = route
    ? readField(route, "startLocation", "start_location")
    : null;

  const distance = route
    ? readField(route, "totalDistanceKm", "total_distance_km")
    : null;

  const duration = route
    ? readField(
        route,
        "estimatedDurationMinutes",
        "estimated_duration_minutes"
      )
    : null;

  const routeId = route
    ? readField(route, "routeId", "route_id", "id")
    : null;

  const createdAt = route
    ? readField(route, "createdAt", "created_at")
    : null;

  function formatDate(value) {
    if (!value) return "Not available";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return String(value);
    }

    return date.toLocaleString();
  }

  return (
    <div
      className="route-history-page"
      style={{ padding: "24px" }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          flexWrap: "wrap",
          gap: "12px",
          marginBottom: "22px",
        }}
      >
        <div>
          <h2 style={{ margin: 0 }}>Route History</h2>
          <p style={{ color: "#888", marginTop: "6px" }}>
            View your previously optimized delivery routes.
          </p>
        </div>

        <button
          onClick={loadRoutes}
          disabled={loading}
          style={{
            padding: "10px 18px",
            border: "none",
            borderRadius: "8px",
            background: "#7c3aed",
            color: "white",
            cursor: loading ? "not-allowed" : "pointer",
            opacity: loading ? 0.7 : 1,
          }}
        >
          {loading ? "Refreshing..." : "Refresh Routes"}
        </button>
      </div>

      {error && (
        <div
          style={{
            padding: "12px",
            marginBottom: "16px",
            background: "#fee2e2",
            color: "#991b1b",
            borderRadius: "8px",
          }}
        >
          {error}
        </div>
      )}

      {loading ? (
        <p>Loading route history...</p>
      ) : routes.length === 0 ? (
        <div
          style={{
            padding: "30px",
            textAlign: "center",
            background: "var(--card-bg, #f8f8f8)",
            borderRadius: "12px",
          }}
        >
          <h3>No saved routes found</h3>
          <p>Optimize a delivery route to see it here.</p>
        </div>
      ) : (
        <div
          style={{
            display: "grid",
            gridTemplateColumns:
              "repeat(auto-fit, minmax(min(100%, 320px), 1fr))",
            gap: "20px",
            alignItems: "start",
          }}
        >
          <section>
            <h3>Saved Routes ({routes.length})</h3>

            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "12px",
              }}
            >
              {routes.map((item) => {
                const id = readField(
                  item,
                  "routeId",
                  "route_id",
                  "id"
                );

                const start = readField(
                  item,
                  "startLocation",
                  "start_location"
                );

                const km = readField(
                  item,
                  "totalDistanceKm",
                  "total_distance_km"
                );

                const date = readField(
                  item,
                  "createdAt",
                  "created_at"
                );

                return (
                  <button
                    key={id}
                    onClick={() => openRoute(item)}
                    style={{
                      textAlign: "left",
                      padding: "16px",
                      borderRadius: "10px",
                      border:
                        String(routeId) === String(id)
                          ? "2px solid #7c3aed"
                          : "1px solid #ddd",
                      background: "var(--card-bg, white)",
                      cursor: "pointer",
                      color: "inherit",
                    }}
                  >
                    <strong>Route #{id}</strong>

                    <p style={{ margin: "8px 0" }}>
                      Starting point: {start || "Not available"}
                    </p>

                    <p style={{ margin: "8px 0" }}>
                      Distance: {km ?? "N/A"} km
                    </p>

                    <small style={{ color: "#888" }}>
                      {formatDate(date)}
                    </small>
                  </button>
                );
              })}
            </div>
          </section>

          <section>
            {detailsLoading ? (
              <p>Loading selected route...</p>
            ) : !route ? (
              <div
                style={{
                  padding: "35px",
                  textAlign: "center",
                  border: "1px dashed #aaa",
                  borderRadius: "12px",
                }}
              >
                <h3>Select a saved route</h3>
                <p>
                  Choose a route from the list to view its details and map.
                </p>
              </div>
            ) : (
              <>
                <h3>Route #{routeId} Details</h3>

                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns:
                      "repeat(auto-fit, minmax(130px, 1fr))",
                    gap: "12px",
                    marginBottom: "18px",
                  }}
                >
                  <div
                    style={{
                      padding: "15px",
                      borderRadius: "10px",
                      background: "var(--card-bg, #f3f0ff)",
                    }}
                  >
                    <small>Starting Location</small>
                    <h3>{startLocation || "N/A"}</h3>
                  </div>

                  <div
                    style={{
                      padding: "15px",
                      borderRadius: "10px",
                      background: "var(--card-bg, #f3f0ff)",
                    }}
                  >
                    <small>Total Distance</small>
                    <h3>{distance ?? "N/A"} km</h3>
                  </div>

                  <div
                    style={{
                      padding: "15px",
                      borderRadius: "10px",
                      background: "var(--card-bg, #f3f0ff)",
                    }}
                  >
                    <small>Estimated Duration</small>
                    <h3>{duration ?? "N/A"} min</h3>
                  </div>
                </div>

                <p>
                  <strong>Created:</strong> {formatDate(createdAt)}
                </p>

                <h3>Optimized Delivery Stops</h3>

                {optimizedStops.length === 0 ? (
                  <p>No stop details were saved for this route.</p>
                ) : (
                  <ol>
                    {optimizedStops.map((stop, index) => (
                      <li
                        key={index}
                        style={{ marginBottom: "8px" }}
                      >
                        {readField(
                          stop,
                          "name",
                          "location",
                          "city"
                        ) || `Stop ${index + 1}`}
                      </li>
                    ))}
                  </ol>
                )}

                <h3>Route Map</h3>

                {mapPositions.length > 0 ? (
                  <div
                    style={{
                      height: "430px",
                      width: "100%",
                      borderRadius: "12px",
                      overflow: "hidden",
                    }}
                  >
                    <MapContainer
                      key={String(routeId)}
                      center={mapPositions[0]}
                      zoom={6}
                      style={{
                        height: "100%",
                        width: "100%",
                      }}
                    >
                      <TileLayer
                        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                      />

                      <FitRoute positions={mapPositions} />

                      {routePositions.length > 1 && (
                        <Polyline
                          positions={routePositions}
                          pathOptions={{
                            color: "#7c3aed",
                            weight: 5,
                          }}
                        />
                      )}

                      {stopPositions.map((item, index) => (
                        <Marker
                          key={index}
                          position={item.position}
                          icon={createMarker(index + 1)}
                        >
                          <Popup>
                            {readField(
                              item.stop,
                              "name",
                              "location",
                              "city"
                            ) || `Stop ${index + 1}`}
                          </Popup>
                        </Marker>
                      ))}
                    </MapContainer>
                  </div>
                ) : (
                  <p>
                    Map coordinates are not available for this route.
                  </p>
                )}
              </>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

export default RouteHistory;
