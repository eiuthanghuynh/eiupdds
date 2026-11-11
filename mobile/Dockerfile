# ==============================================================================
# Production Nginx Dockerfile for EIU PDDS Mobile Client (Web Preview)
# Serves the exported Expo Web bundle with SPA routing & Backend reverse proxy
# ==============================================================================

FROM nginx:alpine

# Remove default nginx welcome page
RUN rm -rf /usr/share/nginx/html/*

# Copy custom Nginx configuration with SPA routing & Backend reverse proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy pre-built static export
COPY dist /usr/share/nginx/html

# Expose HTTP port
EXPOSE 80

# Health check
HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
  CMD wget -qO- http://127.0.0.1:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
