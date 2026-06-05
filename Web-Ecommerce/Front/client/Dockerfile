FROM node:20-alpine AS build
WORKDIR /src
COPY client/package*.json ./client/
COPY shared/ ./shared/
WORKDIR /src/client
RUN npm ci
RUN ln -s /src/client/node_modules /src/node_modules
COPY client/ .
RUN npx vite build

FROM nginx:alpine
COPY --from=build /src/client/dist /usr/share/nginx/html
COPY client/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
