package vuong20194412.chat.authentication_api_gateway_service.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

public class UserErrorException extends RuntimeException {

    private final HttpStatusCode statusCode;

    private final String title;

    private final HttpHeaders headers;

    public UserErrorException(HttpStatusCode statusCode, String title, String message, HttpHeaders headers) {
        super(message);
        this.statusCode = statusCode;
        this.title = title;
        this.headers = headers;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getTitle() {
        return title;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

}
