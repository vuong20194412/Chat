package vuong20194412.chat.authentication_api_gateway_service.advice;

import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vuong20194412.chat.authentication_api_gateway_service.exception.AccountUnprocessableEntityException;

@RestControllerAdvice
class AccountUnprocessableEntityAdvice {

    @ExceptionHandler(AccountUnprocessableEntityException.class)
    ResponseEntity<?> handleAccountUnprocessableEntity(@NonNull AccountUnprocessableEntityException exception) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE)
                .body(Problem.create().withTitle("Unprocessable entity").withDetail(exception.getMessage()));
    }

}
