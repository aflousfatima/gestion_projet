import "../styles/Navbar.css";
import Link from "next/link";

const Navbar = () => {
  return (
    <div>
      <nav className="navbar">
        <div className="container">
          <img className="navbar-logo" src="/logo.png" alt="logo" />
          <ul className="navbar-nav">
            <li className="nav-item">
              <Link className="nav-link" href="/">
                Home
              </Link>
            </li>
            <li className="nav-item">
              <Link className="nav-link" href="/authentification/signin">
                Product
              </Link>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="#">
                Entreprise
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="#">
                Resources
              </a>
            </li>
            <li className="nav-item">
              <a className="nav-link" href="#">
                Pricing
              </a>
            </li>
          </ul>
          <div className="auth-buttons">
            <Link
              href="/authentification/signin"
              className="nav-link btn style-login"
            >
              Log In
            </Link>
            <Link
              href="/authentification/signup"
              className="nav-link btn style-try"
            >
              Start for Free
            </Link>
          </div>
        </div>
      </nav>
    </div>
  );
};

export default Navbar;
