# Stage 1: Build the React Application
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files (from current webapp folder)
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci

# Copy the rest of the frontend code
COPY . .

# Build Arguments (to pass environment variables during build)
ARG REACT_APP_BASE_URL
ARG REACT_APP_SECURE_BASE_URL
ARG REACT_APP_USE_TLS
ARG REACT_APP_ENVIRONMENT

# Build the static files
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Copy built assets from builder stage
COPY --from=builder /app/build /usr/share/nginx/html

# Copy custom Nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]