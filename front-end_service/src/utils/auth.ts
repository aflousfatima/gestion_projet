export const getAccessToken = () => localStorage.getItem("accessToken");

export const isAuthenticated = () => {
    const token = getAccessToken();
    return !!token; // Retourne true si le token est présent
};

export const logout = () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    window.location.href = "/authentification/signin";
};
