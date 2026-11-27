
import OptimizationPage from "../components/OptimizationPage";

function FIFOOptimization() {
  return (
    <OptimizationPage
      algorithm="fifo"
      title="FIFO Optimization"
      description="Select pending orders in their original arrival order while respecting vehicle capacity."
    />
  );
}

export default FIFOOptimization;
