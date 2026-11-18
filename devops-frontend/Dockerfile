FROM node:26-alpine AS build

WORKDIR /app
ARG VITE_TURNSTILE_SITE_KEY
ENV VITE_TURNSTILE_SITE_KEY=$VITE_TURNSTILE_SITE_KEY

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build


FROM nginx:alpine

COPY --from=build /app/dist/ /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.conf /etc/nginx/snippets/security-headers.conf

# Directorios escribibles para ejecutar nginx sin privilegios.
RUN chown -R 101:101 /var/cache/nginx /var/run

USER 101:101

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]