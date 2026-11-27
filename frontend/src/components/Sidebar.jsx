const menuGroups = [
  { title: "Workspace", items: [
    ["Dashboard", "▦"], ["Orders", "▤"], ["Vehicles", "▰"]
  ]},
  { title: "Optimization", items: [
    ["FIFO Optimization", "↗"], ["Greedy Optimization", "⌁"],
    ["Knapsack Optimization", "◈"], ["Algorithm Comparison", "⇄"],
    ["Route Optimization", "⌖"]
  ]},
  { title: "Records", items: [
    ["Assignment History", "◷"], ["Optimization History", "▧"],
    ["Route History", "⌖"]
  ]}
];

function Sidebar({ activePage, setActivePage }) {
  return (
    <aside className="sidebar">
      <div className="brand-lockup">
        <div className="brand-mark">L</div>
        <div>
          <h2 className="sidebar-title">LoadIQ</h2>
          <p className="sidebar-subtitle">INTELLIGENT LOGISTICS</p>
        </div>
      </div>

      {menuGroups.map((group) => (
        <div key={group.title}>
          <div className="sidebar-section-label">{group.title}</div>
          <nav>
            {group.items.map(([item, icon]) => (
              <button
                key={item}
                className={`sidebar-link ${activePage === item ? "active" : ""}`}
                onClick={() => setActivePage(item)}
                type="button"
              >
                <span className="sidebar-icon" aria-hidden="true">{icon}</span>
                <span>{item}</span>
              </button>
            ))}
          </nav>
        </div>
      ))}

      <div className="sidebar-footer">
        <strong>LoadIQ Logistics Suite</strong>
        Smart delivery planning, capacity-aware allocation and route intelligence.
      </div>
    </aside>
  );
}

export default Sidebar;
