# Sirius Nightsky Middleware

REST interface (maybe also sockets) to communicate with the frontend.
This provides access to project as well as to different computation services.
It should be possible to be local as well as remote

## Prerequisites
* gradle
* java 21

## Project setup

### JxBrowser (license)
To start the GUI a JxBrowser license key is needed.
The key needs to be added to your `gradle.properties` and must never be committed to the code repo.

```properties
jxbrowser.license.key = <KEY>
```

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
