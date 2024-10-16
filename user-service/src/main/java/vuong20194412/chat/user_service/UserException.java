package vuong20194412.chat.user_service;

class UserNotFoundException extends RuntimeException {

    UserNotFoundException(Long id) {
        super("Could not find user " + id);
    }
}

class UserUnprocessableEntityException extends RuntimeException {
    enum Type {
        INVALID_EMAIL,
        MISSING_FIELD,
        VALUE_UNAVAILABLE,
    }

    UserUnprocessableEntityException(Type type, String field) {
        super("Could not process - error " + type + " at field " + field);
    }
}