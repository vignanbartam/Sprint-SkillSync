FROM node:24-alpine AS build
WORKDIR /app

RUN npm install -g npm@11.5.1
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM node:24-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=4000

COPY --from=build /app/dist/skillsync-frontend ./dist/skillsync-frontend

EXPOSE 4000
CMD ["node", "dist/skillsync-frontend/server/server.mjs"]
