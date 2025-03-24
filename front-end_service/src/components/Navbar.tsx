import "../styles/Navbar.css";
import Link from "next/link";
const Navbar = () => {
  return (
    <div>
      <nav className="navbar navbar-expand-lg ">
        <div className="container">
          <img className="navbar-logo" src="/logo.png" alt="logo"></img>
          <button
            className="navbar-toggler"
            type="button"
            data-bs-toggle="collapse"
            data-bs-target="#navbarSupportedContent"
            aria-controls="navbarSupportedContent"
            aria-expanded="false"
            aria-label="Toggle navigation"
          >
            <span className="navbar-toggler-icon"></span>
          </button>
          <div className="navbar-collapse" id="navbarSupportedContent">
            <ul className="navbar-nav me-auto mb-2 mb-lg-0">
              <li className="nav-item">
                <Link className="nav-link active" aria-current="page" href="/">
                  Home
                </Link>
              </li>
              <li className="nav-item">
                <Link
                  className="nav-link active"
                  aria-current="page"
                  href="/authentification/signin"
                >
                  Product
                </Link>
              </li>
              <li className="nav-item">
                <a className="nav-link active" aria-current="page" href="#">
                  Entreprise
                </a>
              </li>
              <li className="nav-item">
                <a className="nav-link active" aria-current="page" href="#">
                  Resources
                </a>
              </li>
              <li className="nav-item">
                <a className="nav-link active" aria-current="page" href="#">
                  Pricing
                </a>
              </li>
              <li className="style-login">
                <Link
                  href="/authentification/signup"
                  className="nav-link active btn"
                  aria-current="page"
                >
                  Log In
                </Link>
              </li>
              <li className="style-try">
                <Link
                  href="/authentification/signup"
                  className="nav-link active btn"
                  aria-current="page"
                >
                  Start for Free
                </Link>
              </li>
            </ul>
          </div>
        </div>
      </nav>
    </div>
  );
};
export default Navbar;
