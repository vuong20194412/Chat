package vuong20194412.chat.user_service;

import java.util.List;
import java.util.Map;

class UserNotFoundException extends RuntimeException {

    UserNotFoundException(Long id) {
        super("Could not find user " + id);
    }
}

class UserUnprocessableEntityException extends RuntimeException {

    enum Type {
        INVALID_EMAIL,
        EXISTED_EMAIL,
        MISSING_FIELD,
        VALUE_UNAVAILABLE,
    }

    public UserUnprocessableEntityException(Map<Type, List<String>> errors) {
        super("Could not process - " + String.join("; ", errors.entrySet().stream().map(error -> {
            Type type = error.getKey();
            List<String> fields = error.getValue();
            return String.format("error %s at field %s", type, String.join(",", fields));
        }).toList()));
    }

    UserUnprocessableEntityException(Type type, String field) {
        super("Could not process - error " + type + " at field " + field);
    }

}

class UserUnauthorizedException extends RuntimeException {

    UserUnauthorizedException(String message) {
        super(message);
    }

    UserUnauthorizedException() {
        super("Could not change different user");
    }

}