package vuong20194412.chat.authentication_api_gateway_service;

import jakarta.mail.internet.AddressException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vuong20194412.chat.authentication_api_gateway_service.dto.EntityDTO;
import vuong20194412.chat.authentication_api_gateway_service.dto.PasswordDTO;
import vuong20194412.chat.authentication_api_gateway_service.dto.SignupDTO;
import vuong20194412.chat.authentication_api_gateway_service.entity.TransientPassword;
import vuong20194412.chat.authentication_api_gateway_service.exception.AccountUnprocessableEntityException;
import vuong20194412.chat.authentication_api_gateway_service.entity.Account;
import vuong20194412.chat.authentication_api_gateway_service.service.AccountService;
import vuong20194412.chat.authentication_api_gateway_service.service.JwtService;
import vuong20194412.chat.authentication_api_gateway_service.util.DomainAddressUtil;
import vuong20194412.chat.authentication_api_gateway_service.util.MailUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@RestController
class AccountController {

    private final AccountService service;

    private final JwtService jwtService;

    private final MailUtil mailUtils;

    @Value("${JWT_VALIDITY_PERIOD}")
    private Long jwtValidityPeriod;

    private final String domainAddress;

    @Autowired
    public AccountController(AccountService service, JwtService jwtService, MailUtil mailUtils, DomainAddressUtil domainAddressConfig) {
        this.service = service;
        this.jwtService = jwtService;
        this.mailUtils = mailUtils;
        this.domainAddress = domainAddressConfig.getDomainAddress();
    }

    /**
     * Sign in - deploy in security chain
     *
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:} curl -v -X POST localhost:8000/api/signin -H "content-type:application/json"
     * -d "{\"password\": \"password\", \"email\": \"testemail@v.vn\"}"
     **/
    @SuppressWarnings("unused")
    private void signIn() {}

    /**
     * Log out - deploy in security chain
     *
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:} curl -v -X POST localhost:8000/api/logout -H "Authorization:Bearer &lt;token&gt;"
     **/
    @SuppressWarnings("unused")
    private void logOut() {}

    /* PART SIGN UP ACCOUNT */

    /**
     * Receive signup request -> Check format -> Send signup confirmation require to signup email
     *
     * @param signupDTO make sure there are password, email, fullname
     * @return Object has http code 200 if success (check format and send email)
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:}
     * curl -v -X POST localhost:8000/api/signup -H "content-type:application/json"
     * -d "{\"password\": \"password\", \"email\": \"testemail@v.vn\", \"fullname\": \"test_fullname\"}"
     */
    @PostMapping({"/api/signup", "/api/signup/"})
    ResponseEntity<?> signUp(@RequestBody SignupDTO signupDTO) {
        Account.AccountRecord accountRecord = service.prepareBeforeCreateAccount(signupDTO.password(), signupDTO.email(), signupDTO.fullname());

        TransientPassword transientPassword = service.saveTransientPassword(accountRecord.password(), Instant.now().getEpochSecond() + 5L * 60);

        Map<String, String> claims = new HashMap<>();
        claims.put("passwordId", String.valueOf(transientPassword.getId()));
        claims.put("randomCode", String.valueOf(transientPassword.getRandomCode()));
        claims.put("email", accountRecord.email());
        claims.put("fullname", accountRecord.fullname());
        sendConfirmLink(accountRecord.email(), claims, ConfirmAction.SIGN_UP);

        return ResponseEntity.ok(String.format("Go to email %s to confirm", accountRecord.email()));
    }

    /**
     * Receive signup confirmation token -> Extract info from token -> create new user -> save new account
     * @param token make sure token is valid and there are email, passwordId in payload json web token
     * @return Object has http code 201 if success (verify token, extract token, create user, save account)
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:}
     * curl -v localhost:8000/api/signup/confirm/&lt;token&gt;
     */
    @GetMapping("/api/signup/confirm/{token}")
    ResponseEntity<?> ConfirmSignUp(@PathVariable String token) {
        Map<String, String> claims = jwtService.extractClaims(token);

        String email = claims.get("email");
        String passwordId = claims.get("passwordId");
        if (!Pattern.matches("^[1-9][0-9]*$", passwordId))
            return ResponseEntity.badRequest().body("Please try change password again.");
        String randomCode = claims.get("randomCode");
        if (randomCode == null)
            return ResponseEntity.badRequest().body("Please try change password again.");

        TransientPassword transientPassword = service.getTransientPassword(Long.parseLong(passwordId), randomCode);
        Account account = service.createAccountWithEncodePassword(transientPassword, email, claims.get("fullname"));
        ResponseEntity<EntityDTO> response = service.createUser(email, claims.get("fullname"));
        Long userId = (Long) Objects.requireNonNull(response.getBody()).get("id");
        account.setUserId(userId);
        service.saveAccount(account);

        return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders())
                .body(String.format("{\"title\": \"Sign up success\", %s", response.getBody()));
    }

    /* END SIGN UP ACCOUNT */

    /* PART CHANGE EMAIL */

    @PutMapping({"/api/account/change-email"})
    ResponseEntity<?> changeEmail(@RequestBody Map<String, String> newEmail) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String email = user.getUsername();

        String _newEmail = service.prepareBeforeChangeEmail(email, newEmail.get("newEmail"));

        Map<String, String> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("newEmail", _newEmail);
        sendConfirmLink(email, claims, ConfirmAction.CHANGE_EMAIL_FROM_CURRENT_EMAIL);

        return ResponseEntity.ok(String.format("Go to email %s to confirm", email));
    }

    @GetMapping("/api/account/change-email/confirm/now/{token}")
    ResponseEntity<?> confirmChangeEmailFromCurrentEmail(@PathVariable String token) {
        Map<String, String> _claims = jwtService.extractClaims(token);

        String email = _claims.get("email");
        if (email == null)
            throw new RuntimeException("Not found email");

        String newEmail = _claims.get("newEmail");
        if (newEmail == null)
            throw new RuntimeException("Not found new email");

        Map<String, String> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("newEmail", newEmail);
        sendConfirmLink(newEmail, claims, ConfirmAction.CHANGE_EMAIL_FROM_NEW_EMAIL);

        return ResponseEntity.ok(String.format("Go to email %s to confirm", newEmail));
    }

    @GetMapping("/api/account/change-email/confirm/new/{token}")
    ResponseEntity<?> confirmChangeEmail(@PathVariable String token) {
        Map<String, String> claims = jwtService.extractClaims(token);

        String newEmail = claims.get("newEmail");
        if (newEmail == null)
            throw new RuntimeException("Not found new email");
        String email = claims.get("email");
        if (email == null)
            throw new RuntimeException("Not found email");

        Account account = service.getRepository().findByEmail(email);

        Map<String, Object> body = new HashMap<>();
        body.put("email", newEmail);

        ResponseEntity<Object> response = service.updateUser(account.getUserId(), body);

        Account _account = service.getRepository().findByEmail(email);

        _account.setEmail(newEmail);

        service.saveAccount(_account);

        long expirationTime = Instant.now().getEpochSecond() + jwtValidityPeriod;
        Map<String, String> _claims = new HashMap<>();
        _claims.put("sub", newEmail);
        _claims.put("exp", String.valueOf(expirationTime));
        String newAuthToken = jwtService.generateJwt(_claims);

        return ResponseEntity.accepted().header(HttpHeaders.AUTHORIZATION, "Bearer " + newAuthToken).body("Change email success");
    }

    /* END PART CHANGE EMAIL */

    /* PART CHANGE PASSWORD */

    @PutMapping({"/api/account/change-password", "/api/account/change-password/"})
    ResponseEntity<?> changePassword(@RequestBody PasswordDTO passwordDTO) {
        PasswordDTO _passwords = service.prepareBeforeChangePassword(passwordDTO);

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String encodedPassword = user.getPassword();

        String email = user.getUsername();

        if (!service.getPasswordEncoder().matches(_passwords.rawPassword(), encodedPassword))
            throw new AccountUnprocessableEntityException(AccountUnprocessableEntityException.Type.NO_MATCH_PASSWORDS, "password");

        TransientPassword transientPassword = service.saveTransientPassword(_passwords.rawNewPassword(), Instant.now().getEpochSecond() + 5L * 60);

        Map<String, String> claims = new HashMap<>();
        claims.put("passwordId", String.valueOf(transientPassword.getId()));
        claims.put("randomCode", String.valueOf(transientPassword.getRandomCode()));
        claims.put("email", email);
        sendConfirmLink(email, claims, ConfirmAction.CHANGE_PASSWORD);

        return ResponseEntity.ok(String.format("Go to email %s to confirm", email));
    }

    @GetMapping("/api/account/change-password/confirm/{token}")
    ResponseEntity<?> confirmChangePassword(@PathVariable String token) {
        Map<String, String> claims = jwtService.extractClaims(token);

        String email = claims.get("email");
        String passwordId = claims.get("passwordId");
        String randomCode = claims.get("randomCode");

        if (!Pattern.matches("^[1-9][0-9]*$", passwordId))
            return ResponseEntity.badRequest().body("Please try change password again.");
        if (randomCode == null)
            return ResponseEntity.badRequest().body("Please try change password again.");

        TransientPassword transientPassword = service.getTransientPassword(Long.parseLong(passwordId), randomCode);
        service.changePasswordWithEncodePassword(transientPassword, email);

        return ResponseEntity.ok("Change password success");
    }

    /* END PART CHANGE PASSWORD */

    /* PART RESET PASSWORD */

    @PostMapping({"/api/account/reset-password", "/api/account/forget-password/"})
    ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email") != null ? body.get("email").trim().toLowerCase() : "";
        if (email.isBlank())
            throw new AccountUnprocessableEntityException(AccountUnprocessableEntityException.Type.MISSING_FIELD, "email");

        if (!service.getRepository().existsByEmail(email))
            return ResponseEntity.ok(String.format("Go to email %s to confirm if registered by this email", email));

        Map<String, String> claims = new HashMap<>();
        claims.put("email", email);
        sendConfirmLink(email, claims, ConfirmAction.RESET_PASSWORD);

        return ResponseEntity.ok(String.format("Go to email %s to confirm if registered by this email", email));
    }

    @GetMapping("/api/account/reset-password/confirm/{token}")
    ResponseEntity<?> createNewPassword(@PathVariable String token) {
        Map<String, String> claims = jwtService.extractClaims(token);

        String email = claims.get("email");

        String newPassword = service.generatePassword();

        Account account = service.getRepository().findByEmail(email);
        account.setPassword(service.getPasswordEncoder().encode(newPassword));
        service.saveAccount(account);

        try {
            mailUtils.sendHtml(email, "New password - V", "<p>New your password is </p>" + newPassword);
        } catch (AddressException e) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.accepted().body("Reset password success. Please check email.");
    }

    /* END PART RESET PASSWORD */

    /* PART DELETE ACCOUNT */

    @DeleteMapping({"/api/account/remove", "/api/account/remove/"})
    ResponseEntity<?> delete() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String email = user.getUsername();

        Map<String, String> claims = new HashMap<>();
        claims.put("email", email);
        sendConfirmLink(email, claims, ConfirmAction.DELETE_ACCOUNT);

        return ResponseEntity.ok(String.format("Go to email %s to confirm", email));
    }

    @GetMapping("/api/account/remove/confirm/{token}")
    ResponseEntity<?> confirmDelete(@PathVariable String token) {
        Map<String, String> claims = jwtService.extractClaims(token);

        String email = claims.get("email");

        Account account = service.getRepository().findByEmail(email);
        if (account == null)
            return ResponseEntity.noContent().build();

        Long userId = account.getUserId();

        service.deleteUser(userId);

        service.getRepository().deleteById(userId);

        return ResponseEntity.noContent().build();
    }

    /* END PART DELETE ACCOUNT */

    /* PRIVATE: PART SEND CONFIRM */

    private enum ConfirmAction {
        SIGN_UP,
        CHANGE_EMAIL_FROM_CURRENT_EMAIL,
        CHANGE_EMAIL_FROM_NEW_EMAIL,
        CHANGE_PASSWORD,
        RESET_PASSWORD,
        DELETE_ACCOUNT,
    }

    private void sendConfirmLink(String recipientEmail, Map<String, String> claims, ConfirmAction action) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss' UTC'").withZone(ZoneId.of("UTC"));
        Instant instant = Instant.now().plusSeconds(5 * 60);
        String expirationTime = dateTimeFormatter.format(instant);
        claims.put("exp", String.valueOf(instant.getEpochSecond()));
        String token = jwtService.generateJwtOneTime(claims);
        String emailSubject;
        String content;

        if (action == ConfirmAction.SIGN_UP) {
            emailSubject = "Sign up - V";
            String link = String.format("http://%s/api/signup/confirm/%s", domainAddress, token);
            content =  getContent(link, expirationTime, "Sign up");
        }
        else if (action == ConfirmAction.CHANGE_PASSWORD) {
            emailSubject = "Change password - V";
            String link = String.format("http://%s/api/account/change-password/confirm/%s", domainAddress, token);
            content =  getContent(link, expirationTime, "Change password");
        }
        else if (action == ConfirmAction.RESET_PASSWORD) {
            emailSubject = "Reset password - V";
            String link = String.format("http://%s/api/account/reset-password/confirm/%s", domainAddress, token);
            content =  getContent(link, expirationTime, "Reset password");
        }
        else if (action == ConfirmAction.CHANGE_EMAIL_FROM_CURRENT_EMAIL) {
            emailSubject = "Change email - V";
            String link = String.format("http://%s/api/account/change-email/confirm/now/%s", domainAddress, token);
            content =  getContent(link, expirationTime, "Change email");
        }
        else if (action == ConfirmAction.CHANGE_EMAIL_FROM_NEW_EMAIL) {
            emailSubject = "Change email - V";
            String link = String.format("http://%s/api/account/change-email/confirm/new/%s", domainAddress, token);
            content =  getContent(link, expirationTime, "Change email");
        }
        else if (action == ConfirmAction.DELETE_ACCOUNT) {
            emailSubject = "Remove account - V";
            String link = String.format("http://%s/api/account/remove/confirm/%s", domainAddress, token);
            content =  getContent(link, expirationTime, "Remove account");
        }
        else {
            throw new RuntimeException("Action");
        }

        try {
            mailUtils.sendHtml(recipientEmail, emailSubject, content);
        } catch (AddressException e) {
            throw new AccountUnprocessableEntityException(AccountUnprocessableEntityException.Type.INVALID_EMAIL, "recipientEmail");
        }
    }

    private String getContent(String link, String expirationTime, String confirmAction) {
        String toAsk = String.format("If the request is from yourself, please confirm before %s. Otherwise please ignore this message.", expirationTime);
        String toClick = String.format("<a href=\"%s\">%s</a>", link, confirmAction);
        String toCopy = String.format("If link above does not appear, please copy and paste this link into your browser's address bar: %s", link);
        return "<p>Hi</p>" +
                "<br>" +
                "<p>We got a request to perform an action that requires your confirmation.<p>" +
                "<p>" + toAsk + "</p>" +
                toClick +
                "<p>" + toCopy + "</p>";
    }

    /* PRIVATE: END PART SEND CONFIRM */

}