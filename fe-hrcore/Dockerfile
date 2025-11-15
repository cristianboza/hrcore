# fe-hrcore Dockerfile - build React app and serve with nginx

FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --silent
COPY . .
RUN npm run build

FROM nginx:stable-alpine

# Copy built files
COPY --from=build /app/dist /usr/share/nginx/html

# Copy nginx config template
COPY nginx.conf /etc/nginx/templates/default.conf.template

# Install gettext for envsubst
RUN apk add --no-cache gettext

# Create entrypoint script that substitutes env vars
RUN printf '#!/bin/sh\n\
set -e\n\
\n\
# Process template and substitute environment variables\n\
envsubst "$(env | cut -d= -f1 | sed -e "s/^/\$/g")" \\\n\
  < /etc/nginx/templates/default.conf.template \\\n\
  > /etc/nginx/conf.d/default.conf\n\
\n\
# Start nginx\n\
exec nginx -g "daemon off;"\n' > /docker-entrypoint.sh && \
    chmod +x /docker-entrypoint.sh

EXPOSE 80

CMD ["/docker-entrypoint.sh"]