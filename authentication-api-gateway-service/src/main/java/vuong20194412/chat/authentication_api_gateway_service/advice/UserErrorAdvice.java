package vuong20194412.chat.authentication_api_gateway_service.advice;

import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vuong20194412.chat.authentication_api_gateway_service.exception.UserErrorException;

@RestControllerAdvice
public class UserErrorAdvice {

    @ExceptionHandler(UserErrorException.class)
    ResponseEntity<?> handleUserError(@NonNull UserErrorException exception) {
        ResponseEntity.BodyBuilder builder = ResponseEntity
                .status(exception.getStatusCode());

        if (exception.getHeaders() != null) {
            builder.headers(exception.getHeaders());
        }

        return builder
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE)
                .body(Problem.create().withTitle(exception.getTitle()).withDetail(exception.getMessage()));
    }

}
