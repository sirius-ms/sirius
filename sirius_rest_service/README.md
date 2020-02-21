# Sirius Nightsky Middleware

REST interface (maybe also sockets) to communicate with the frontend.
This provides access to projectspace as well as to different computation services.
It should be possible to be local as well as remote

## Prerequisites
* gradle
* java 11

## Project setup

?

## Project Scripts

#### start application
```
gradle bootRun
```

#### build jar
```
gradle bootJar
```

#### build jar with the frontend included
```
gradle bootJarWithFrontend
```
This will also 'build' the frontend and copy the frontend-dist-directory into the build/resources/main/public-directory.
The final jar will also include the frontend, so it can serve this when started in a java-web-server.

## Integrated API-endpoints

Swagger2 API definition:
http://localhost:8080/v2/api-docs

UI for API:
http://localhost:8080/swagger-ui.html
