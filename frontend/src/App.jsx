
import { useState } from "react";

import Sidebar from "./components/Sidebar";
import Navbar from "./components/Navbar";

import Dashboard from "./pages/Dashboard";
import Orders from "./pages/Orders";
import Vehicles from "./pages/Vehicles";
import FIFOOptimization from "./pages/FIFOOptimization";
import GreedyOptimization from "./pages/GreedyOptimization";
import KnapsackOptimization from "./pages/KnapsackOptimization";
import AlgorithmComparison from "./pages/AlgorithmComparison";
import RouteOptimization from "./pages/RouteOptimization";
import AssignmentHistory from "./pages/AssignmentHistory";
import OptimizationHistory from "./pages/OptimizationHistory";
import RouteHistory from "./pages/RouteHistory";

import "./App.css";

function App() {
  const [activePage, setActivePage] = useState("Dashboard");

  const renderPage = () => {
    switch (activePage) {
      case "Orders":
        return <Orders />;

      case "Vehicles":
        return <Vehicles />;

      case "FIFO Optimization":
        return <FIFOOptimization />;

      case "Greedy Optimization":
        return <GreedyOptimization />;

      case "Knapsack Optimization":
        return <KnapsackOptimization />;

      case "Algorithm Comparison":
        return <AlgorithmComparison />;

      case "Route Optimization":
        return <RouteOptimization />;

      case "Assignment History":
        return <AssignmentHistory />;

      case "Optimization History":
        return <OptimizationHistory />;

      case "Route History":
        return <RouteHistory />;

      default:
        return <Dashboard />;
    }
  };

  return (
    <div className="app-layout">
      <Sidebar
        activePage={activePage}
        setActivePage={setActivePage}
      />

      <main className="main-content">
        <Navbar activePage={activePage} />
        {renderPage()}
      </main>
    </div>
  );
}

export default App;
