function Navbar({ activePage }) {
  return (
    <header className="navbar">
      <div>
        <h2>{activePage}</h2>
        <p>LoadIQ <span aria-hidden="true">/</span> Intelligent Logistics Management</p>
      </div>
      <div className="navbar-right">
        <span className="status-indicator" />
        <span>System Online</span>
      </div>
    </header>
  );
}

export default Navbar;
