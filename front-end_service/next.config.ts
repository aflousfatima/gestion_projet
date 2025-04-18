import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  env: {
    VAULT_URL: process.env.VAULT_URL,
    VAULT_TOKEN: process.env.VAULT_TOKEN,
}, /* config options here */
};

module.exports = {
  async rewrites() {
    return [
      {
        source: "/ws/:path*",
        destination: "http://localhost:8086/ws/:path*",
      },
    ];
  },
};
export default nextConfig;
