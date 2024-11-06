package vuong20194412.chat.authentication_api_gateway_service.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException() {
        super("Could not find user");
    }

}
