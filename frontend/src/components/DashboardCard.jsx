function DashboardCard({ title, value, description }) {
  return (
    <article className="dashboard-card">
      <h3>{title}</h3>
      <h2>{value}</h2>
      <p>{description}</p>
    </article>
  );
}

export default DashboardCard;
