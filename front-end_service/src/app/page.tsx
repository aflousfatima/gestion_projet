import "../styles/Home.css";

const Home = () => {
  return (
    <div className="container hero-section">
      <div className="row align-items-center">
        {/* Texte à gauche */}
        <div className="col-md-6 text-start">
          <h1 className="headline">
            Take Your Projects to the Next Level{" "}
            <i className="fas fa-arrow-up"></i>
          </h1>
          <p className="description">
            Tired of complexity? <br />
            Transform the way you manage projects—fast, simple, and effective.{" "}
            <br />
            With <span className="highlight">AGILIA</span>, collaboration is
            seamless, and delivery is faster.
          </p>
          <div className="hero-buttons">
            <a href="#" className="btn btn-primary me-3">
              <i className="fas fa-play-circle"></i> Try it for Free – Get Started
              Today
            </a>
          </div>
        </div>

        {/* Image à droite */}
        <div className="col-md-6 ">
          <img
            src="/home_picture.png"
            alt="Olivia Smith"
            className="hero-image"
          />
        </div>
        <div className="">
          <img
            src="/separateur.png"
            alt="Olivia Smith"
            className="separateur-image"
          />
        </div>
        <h2 className="style-reasons">3 Reasons for Choosing Us</h2>
        <div className="cards-container">
          <div className="card">
            <div className="icon">
              <i className="fas fa-cogs"></i>
            </div>
            <div className="title">Efficiency</div>
            <div className="description">
              Optimize your workflow, eliminate bottlenecks, and deliver results
              faster than ever.
            </div>
            <a href="#" className="read-more">
              Read more <i className="fas fa-arrow-right"></i>
            </a>
          </div>
          <div className="card">
            <div className="icon">
              <i className="fas fa-users"></i>
            </div>
            <div className="title">Collaboration</div>
            <div className="description">
              Seamless teamwork with real-time updates, ensuring everyone stays
              on the same page.
            </div>
            <a href="#" className="read-more">
              Read more <i className="fas fa-arrow-right"></i>
            </a>
          </div>
          <div className="card">
            <div className="icon">
              <i className="fas fa-expand"></i>
            </div>
            <div className="title">Flexibility</div>
            <div className="description">
              A solution that adapts to your needs, whether you are a startup or
              a growing enterprise.
            </div>
            <a href="#" className="read-more">
              Read more <i className="fas fa-arrow-right"></i>
            </a>
          </div>
        </div>

        <div className="container-image">
          <div className="">
          <img
            src="/section4.png"
            alt="Olivia Smith"
            className="section4-image"
          />
        </div>
        <div className="">
          <img
            src="/feedback.png"
            alt="feed back"
            className="feedback-image"
          />
          </div>
        </div>
        <div className="section-text">
          <h1>Transform Your Projects with Seamless Agile Management</h1>
          <p>
In today’s fast-paced world, project management needs to be efficient, collaborative, and flexible.
 Our platform provides everything you need to streamline your workflows, boost team productivity, and adapt quickly to changing requirements.
  Whether you are working on a small task or a large-scale project, our tools make it easy to plan, track progress, and deliver results on time. 
  With real-time updates and an intuitive interface, your team can stay aligned, communicate effectively, and achieve goals with confidence. 
  Start managing your projects smarter today!
          </p>
          <button className="cta-button">Get Started</button>
          </div >
      </div>
    </div>
  );
};

export default Home;
