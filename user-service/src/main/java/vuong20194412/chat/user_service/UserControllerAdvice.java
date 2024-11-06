package vuong20194412.chat.user_service;

import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class UserNotFoundAdvice {

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<?> handleUserNotFound(@NonNull UserNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE)
                .body(Problem.create().withTitle("Not found").withDetail(exception.getMessage()));
    }

}

@RestControllerAdvice
class UserUnprocessableEntityAdvice {

    @ExceptionHandler(UserUnprocessableEntityException.class)
    ResponseEntity<?> handleUserUnprocessableEntity(@NonNull UserUnprocessableEntityException exception) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE)
                .body(Problem.create().withTitle("Unprocessable entity").withDetail(exception.getMessage()));
    }

}

@RestControllerAdvice
class UserUnauthorizedAdvice {

    @ExceptionHandler(UserUnauthorizedException.class)
    ResponseEntity<?> handleUserUnauthorized(@NonNull UserUnauthorizedException exception) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE)
                .body(Problem.create().withTitle("Unauthorized").withDetail(exception.getMessage()));
    }

}
