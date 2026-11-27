# LoadIQ – Delivery Load Optimisation Engine

LoadIQ is a delivery load optimisation system designed to allocate customer orders to delivery vehicles efficiently while respecting weight and volume capacity constraints.

## Key Features

- Order management
- Vehicle management
- Priority-based parcel allocation
- FIFO, Greedy and Knapsack optimisation algorithms
- Weight and volume capacity validation
- Unassigned order tracking
- Load sheets and assignment history
- Route optimisation and route history
- Optimisation performance comparison

## Technology Stack

**Frontend:** React, JavaScript, CSS, Vite

**Backend:** Java 17, Spring Boot, REST API

**Database:** MySQL 8

**Mapping:** Leaflet and OpenStreetMap-based routing

## Project Structure

```text
LoadIQ/
├── backend/
├── frontend/
├── .gitignore
└── README.md
```

## Running Locally

### Backend

Configure the following environment variables:

- `LOADIQ_DB_URL`
- `LOADIQ_DB_USER`
- `LOADIQ_DB_PASSWORD`

Run the Spring Boot application from the backend directory.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Project Objective

To optimise delivery vehicle loading by prioritising important orders, respecting vehicle capacity constraints, and providing efficient allocation and route-planning capabilities.

---

Developed as an individual academic project.
