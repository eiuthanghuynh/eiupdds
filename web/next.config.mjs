/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  transpilePackages: ['antd', '@ant-design/nextjs-registry'],
  images: {
    unoptimized: true,
  },
};

export default nextConfig;
