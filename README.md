# Openpress (v0.1)- by Túlio F. Horta

## Introduction
This project is a simple e-commerce and blogging solution (MVC style) able to provide users with backend and frontend 
implementations. It was idealized in such a way it can be hosted locally even. Furthermore, it allows generic companies 
to digitalize with ease and for cheap. You'll find the app's documentation in the "docs" folder.

## The API
This project was created using the [Ktor Project Generator](https://start.ktor.io).
If you're new to KTor, below there are some useful links with documentation:
* [Ktor Documentation](https://ktor.io/docs/home.html)
* [Ktor GitHub page](https://github.com/ktorio/ktor)

| Name                                                                                  | Description                                                                        |
|---------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| [Compression](https://start.ktor.io/p/io.ktor/server-compression)                     | Compresses responses using encoding algorithms like GZIP                           |
| [Authentication](https://start.ktor.io/p/io.ktor/server-auth)                         | Provides extension point for handling the Authorization header                     |
| [Authentication JWT](https://start.ktor.io/p/io.ktor/server-auth-jwt)                 | Handles JSON Web Token (JWT) bearer authentication scheme                          |
| [Content Negotiation](https://start.ktor.io/p/io.ktor/server-content-negotiation)     | Provides automatic content conversion according to Content-Type and Accept headers |
| [kotlinx.serialization](https://start.ktor.io/p/io.ktor/server-kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library                     |
| [Sessions](https://start.ktor.io/p/io.ktor/server-sessions)                           | Adds support for persistent sessions through cookies or headers                    |
| [CORS](https://start.ktor.io/p/io.ktor/server-cors)                                   | Enables Cross-Origin Resource Sharing (CORS)                                       |
| [Default Headers](https://start.ktor.io/p/io.ktor/server-default-headers)             | Adds a default set of headers to HTTP responses                                    |
| [Simple Cache](https://start.ktor.io/p/com.ucasoft/server-simple-cache)               | Provides API for cache management                                                  |
| [Static Content](https://start.ktor.io/p/io.ktor/server-static-content)               | Serves static files from defined locations                                         |
| [Call Logging](https://start.ktor.io/p/io.ktor/server-call-logging)                   | Logs client requests                                                               |
| [Call ID](https://start.ktor.io/p/io.ktor/server-callid)                              | Allows to identify a request/call.                                                 |
| [PostgreSQL](https://start.ktor.io/p/org.jetbrains/server-postgres)                   | Adds Postgres database support                                                     |

## Frontend
TODO after the backend is done.

## Building & Running
While building the project, you'll need to install the latest version of [PostgreSQL](https://www.postgresql.org/).
When dealing with Openpress, it is recommended to use a Linux distribution to better manage the application.
After that, you can "compile" the project by running the Go executable called "buildMe" within the "builds" folder.
If the generated build starts successfully, you'll see something close to the following output:
```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```
