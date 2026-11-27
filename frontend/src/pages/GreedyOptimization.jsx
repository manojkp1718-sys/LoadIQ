
import OptimizationPage from "../components/OptimizationPage";

function GreedyOptimization() {
  return (
    <OptimizationPage
      algorithm="greedy"
      title="Greedy Priority Optimization"
      description="Select pending orders based on their priority scores while respecting vehicle capacity."
    />
  );
}

export default GreedyOptimization;
