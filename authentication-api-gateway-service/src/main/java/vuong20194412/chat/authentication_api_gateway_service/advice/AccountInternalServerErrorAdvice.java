package vuong20194412.chat.authentication_api_gateway_service.advice;

import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vuong20194412.chat.authentication_api_gateway_service.exception.AccountInternalServerErrorException;

@RestControllerAdvice
class AccountInternalServerErrorAdvice {

    @ExceptionHandler(AccountInternalServerErrorException.class)
    ResponseEntity<?> handleAccountInternalServerError(@NonNull AccountInternalServerErrorException exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE)
                .body(Problem.create().withTitle("Internal server error").withDetail(exception.getMessage()));
    }

}
