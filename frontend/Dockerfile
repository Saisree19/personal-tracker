# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Install deps
COPY package.json package-lock.json ./
RUN npm ci

# Build with provided API endpoints
COPY . .
ARG VITE_AUTH_URL=http://localhost:8081
ARG VITE_TASK_URL=http://localhost:8082
ARG VITE_REPORT_URL=http://localhost:8083
ENV VITE_AUTH_URL=${VITE_AUTH_URL}
ENV VITE_TASK_URL=${VITE_TASK_URL}
ENV VITE_REPORT_URL=${VITE_REPORT_URL}
RUN npm run build

# Runtime stage
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist .
EXPOSE 80
CMD ["nginx","-g","daemon off;"]
