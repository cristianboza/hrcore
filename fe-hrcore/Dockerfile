FROM node:22-alpine AS build
WORKDIR /app

# Copy package files first for dependency caching
COPY package*.json ./
RUN npm ci --silent

# Copy source and build
COPY . .
RUN npm run build

FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html

# Copy built files
COPY --from=build /app/dist .

# Copy nginx config template
COPY nginx.conf /etc/nginx/templates/default.conf.template

# Use nginx's built-in envsubst support (available since nginx:1.19)
# Templates in /etc/nginx/templates/*.template are auto-processed on startup
ENV NGINX_ENVSUBST_OUTPUT_DIR=/etc/nginx/conf.d

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
