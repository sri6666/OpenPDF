FROM node:20-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Create storage directory
RUN mkdir -p storage

# Expose port
EXPOSE 3000

# Start application
CMD ["npm", "run", "dev"]
