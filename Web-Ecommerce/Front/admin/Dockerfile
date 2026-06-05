FROM node:20-alpine AS build
WORKDIR /src
COPY admin/package*.json ./admin/
COPY shared/ ./shared/
WORKDIR /src/admin
RUN npm ci
RUN ln -s /src/admin/node_modules /src/node_modules
COPY admin/ .
RUN npx vite build

FROM nginx:alpine
COPY --from=build /src/admin/dist /usr/share/nginx/html
COPY admin/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
