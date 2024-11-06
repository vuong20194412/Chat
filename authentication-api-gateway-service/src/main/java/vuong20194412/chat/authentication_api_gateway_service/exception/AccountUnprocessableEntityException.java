package vuong20194412.chat.authentication_api_gateway_service.exception;

import java.util.List;
import java.util.Map;

public class AccountUnprocessableEntityException extends RuntimeException {

    public enum Type {
        INVALID_EMAIL,
        EXISTED_EMAIL,
        MISSING_FIELD,
        VALUE_UNAVAILABLE,
        NO_MATCH_PASSWORDS,
        NO_CHANGE,
    }

    public AccountUnprocessableEntityException(Map<Type, List<String>> errors) {
        super("Could not process - " + String.join("; ", errors.entrySet().stream().map(error -> {
            Type type = error.getKey();
            List<String> fields = error.getValue();
            return String.format("error %s at field %s", type, String.join(",", fields));
        }).toList()));
    }

    public AccountUnprocessableEntityException(Type type, String field) {
        super("Could not process - error " + type + " at field " + field);
    }

}