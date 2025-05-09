import axios from "axios";

const axiosInstance = axios.create({
  withCredentials: true, // ✅ Toujours envoyer les cookies si nécessaire
});

export default axiosInstance;
