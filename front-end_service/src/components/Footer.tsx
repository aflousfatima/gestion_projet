import "../styles/Footer.css";

const Footer = () => {
  return (
    <div>
      <footer className="footer mt-5 py-4">
        <div className="container ">
          <div className="row">
            <div className="col-lg-4 col-md-6 mb-4">
              <img
                src="/logo.png"
                alt="AGILIA Logo"
                className="footer-logo mb-3"
              />
              <p className="footer-text">
                Elevate your workflow. Stay organized. Deliver faster.
              </p>
            </div>

            <div className="col-lg-2 col-md-6 mb-4">
              <h5 className="text-uppercase">Quick Links</h5>
              <ul className="list-unstyled">
                <li>
                  <a href="#" className="footer-link">
                    About
                  </a>
                </li>
                <li>
                  <a href="#" className="footer-link">
                    Features
                  </a>
                </li>
                <li>
                  <a href="#" className="footer-link">
                    Pricing
                  </a>
                </li>
                <li>
                  <a href="#" className="footer-link">
                    Contact
                  </a>
                </li>
              </ul>
            </div>

            <div className="col-lg-3 col-md-6 mb-4">
              <h5 className="text-uppercase">Follow Us</h5>
              <div className="d-flex gap-3">
                <a href="#" className="social-icon" aria-label="Facebook">
                  <i className="fab fa-facebook-f" />
                </a>
                <a href="#" className="social-icon" aria-label="Twitter">
                  <i className="fab fa-twitter" />
                </a>
                <a href="#" className="social-icon" aria-label="LinkedIn">
                  <i className="fab fa-linkedin-in" />
                </a>
                <a href="#" className="social-icon" aria-label="GitHub">
                  <i className="fab fa-github" />
                </a>
              </div>
            </div>

            <div className="col-lg-3 col-md-6 mb-4">
              <h5 className="text-uppercase">Stay Updated</h5>
              <form className="d-flex">
                <input
                  type="email"
                  className="form-control me-2"
                  placeholder="Enter your email"
                />
                <button type="submit" className="btn btn-style">
                  Subscribe
                </button>
              </form>
            </div>
          </div>

          <div className="text-center pt-3 border-top">
            <p className="mb-0">
              © 2025 AGILIA. All rights reserved. |{" "}
              <a href="#" className="footer-link">
                Privacy Policy
              </a>{" "}
              |{" "}
              <a href="#" className="footer-link">
                Terms of Service
              </a>
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default Footer;