import Navbar from "./components/Navbar/Navbar";
import Home from "./pages/Home";
import Footer from "./components/Footer/Footer";
const App = () => {
  return (
    <>
      <Navbar />
      <div className="content">
        <Home />
      </div>
      <Footer/>
    </>
  );
};

export default App;
