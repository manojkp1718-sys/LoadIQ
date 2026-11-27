const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

// Common response handler
async function handleResponse(response, fallbackMessage) {
  let data;

  try {
    data = await response.json();
  } catch {
    throw new Error(fallbackMessage);
  }

  if (!response.ok) {
    throw new Error(
      data.message || data.detail || fallbackMessage
    );
  }

  return data;
}

// ==================== ORDERS ====================

export async function getOrders() {
  const response = await fetch(`${API_BASE_URL}/orders`);
  return handleResponse(response, "Failed to fetch orders");
}

export async function addOrder(order) {
  const response = await fetch(`${API_BASE_URL}/orders`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(order),
  });

  return handleResponse(response, "Failed to add order");
}

export async function deleteOrder(id) {
  const response = await fetch(`${API_BASE_URL}/orders/${id}`, {
    method: "DELETE",
  });

  return handleResponse(response, "Failed to delete order");
}

export async function updateOrderStatus(id, status) {
  const response = await fetch(`${API_BASE_URL}/orders/${id}/status`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status }),
  });

  return handleResponse(response, "Failed to update order status");
}

// ==================== VEHICLES ====================

export async function getVehicles() {
  const response = await fetch(`${API_BASE_URL}/vehicles`);
  return handleResponse(response, "Failed to fetch vehicles");
}

export async function addVehicle(vehicle) {
  const response = await fetch(`${API_BASE_URL}/vehicles`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(vehicle),
  });

  return handleResponse(response, "Failed to add vehicle");
}

export async function deleteVehicle(id) {
  const response = await fetch(`${API_BASE_URL}/vehicles/${id}`, {
    method: "DELETE",
  });

  return handleResponse(response, "Failed to delete vehicle");
}

export async function updateVehicleAvailability(id, availability) {
  const response = await fetch(`${API_BASE_URL}/vehicles/${id}/availability`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ availability }),
  });

  return handleResponse(
    response,
    "Failed to update vehicle availability"
  );
}

// ==================== OPTIMIZATION ====================

export async function runOptimization(
  algorithm,
  vehicleId,
  save = false,
  orderIds = null
) {
  const response = await fetch(
    `${API_BASE_URL}/optimization/${algorithm}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        vehicleId: Number(vehicleId),
        save,
        orderIds: Array.isArray(orderIds)
          ? orderIds.map(Number)
          : null,
      }),
    }
  );

  return handleResponse(response, "Optimization failed");
}

export async function compareAlgorithms(vehicleId) {
  const response = await fetch(`${API_BASE_URL}/optimization/compare`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      vehicleId: Number(vehicleId),
    }),
  });

  return handleResponse(response, "Algorithm comparison failed");
}

// ==================== ROUTING ====================

export async function findShortestRoute(start, destination) {
  const response = await fetch(`${API_BASE_URL}/routes/shortest`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ start, destination }),
  });

  return handleResponse(response, "Route calculation failed");
}

export async function findRoadRoute(
  startLatitude,
  startLongitude,
  destinationLatitude,
  destinationLongitude
) {
  const response = await fetch(`${API_BASE_URL}/routes/road`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      startLatitude,
      startLongitude,
      destinationLatitude,
      destinationLongitude,
    }),
  });

  return handleResponse(
    response,
    "Real road route calculation failed"
  );
}

export async function findMultiStopRoute(routeRequest) {
  const response = await fetch(`${API_BASE_URL}/routes/multi-stop`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(routeRequest),
  });

  return handleResponse(
    response,
    "Multi-stop route optimization failed"
  );
}

// ==================== ASSIGNMENT HISTORY ====================

export async function getAssignmentHistory() {
  const response = await fetch(`${API_BASE_URL}/history/assignments`);

  return handleResponse(
    response,
    "Failed to fetch assignment history"
  );
}

// ==================== OPTIMIZATION HISTORY ====================

export async function getOptimizationHistory() {
  const response = await fetch(`${API_BASE_URL}/history/optimizations`);

  return handleResponse(
    response,
    "Failed to fetch optimization history"
  );
}

// ==================== DASHBOARD ====================

export async function getDashboardData() {
  const response = await fetch(`${API_BASE_URL}/dashboard`);

  return handleResponse(
    response,
    "Failed to fetch dashboard data"
  );
}

// ==================== ROUTE HISTORY ====================

export async function getRouteHistory() {
  const response = await fetch(`${API_BASE_URL}/routes/history`);

  return handleResponse(
    response,
    "Failed to fetch route history"
  );
}

export async function getRouteById(routeId) {
  const response = await fetch(
    `${API_BASE_URL}/routes/history/${routeId}`
  );

  return handleResponse(
    response,
    "Failed to fetch route details"
  );
}