rootProject.name = "Chat"

include(
    "eureka-server",
    "user-service",
    //"authentication-service",
    //"api-gateway-service",
    "authentication-api-gateway-service",
    "message-service")