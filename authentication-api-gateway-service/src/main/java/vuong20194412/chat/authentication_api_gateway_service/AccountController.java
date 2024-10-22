package vuong20194412.chat.authentication_api_gateway_service;

import jakarta.mail.internet.AddressException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import vuong20194412.chat.authentication_api_gateway_service.model.Account;
import vuong20194412.chat.authentication_api_gateway_service.model.AccountDTO;
import vuong20194412.chat.authentication_api_gateway_service.repository.AccountRepository;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
class AccountController {

    @Autowired
    private Environment env;

    private final RestTemplate restTemplate;

    private final PasswordEncoder passwordEncoder;

    private final AccountRepository repository;

    private final JwtHS256Utils jwtHS256Utils;

    private final MailUtils mailUtils;

    @Autowired
    public AccountController(RestTemplate restTemplate, PasswordEncoder passwordEncoder, AccountRepository repository, JwtHS256Utils jwtHS256Utils, MailUtils mailUtils) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtHS256Utils = jwtHS256Utils;
        this.mailUtils = mailUtils;
    }

    /**
     * Sign in
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:} curl -v -X POST localhost:8000/api/signin -H "content-type:application/json"
     * -d "{\"password\": \"password\", \"email\": \"testemail@v.vn\"}"
    **/
    private void signIn() {}

    /**
     * Log out
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:} curl -v -X POST localhost:8000/api/logout -H "Authorization:Bearer &lt;token&gt;"
     **/
    private void logOut() {}

    /**
     * Sign up
     * @param accountDTO from body
     * @return Object
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:}
     * curl -v -X POST localhost:8000/api/signup -H "content-type:application/json"
     * -d "{\"password\": \"password\", \"email\": \"testemail@v.vn\", \"fullname\": \"test_fullname\"}"
     */
    @PostMapping({"/api/signup", "/api/signup/"})
    ResponseEntity<?> signUp(@RequestBody AccountDTO accountDTO) {
        if (accountDTO.password() == null || accountDTO.password().isBlank())
            return ResponseEntity.unprocessableEntity().build();

        if (accountDTO.fullname() == null || accountDTO.fullname().isBlank())
            return ResponseEntity.unprocessableEntity().build();

        if (accountDTO.email() == null || accountDTO.email().isBlank())
            return ResponseEntity.unprocessableEntity().build();

        String email = accountDTO.email().trim().toLowerCase();
        if (!mailUtils.isValidEmailAddress(email))
            return ResponseEntity.unprocessableEntity().body("Invalid email");

        if (repository.existsByEmail(email))
            return ResponseEntity.unprocessableEntity().body("Existed Email");

        Map<String, String> claims = new HashMap<>();
        claims.put("password", accountDTO.password());
        claims.put("email", email);
        claims.put("fullname", accountDTO.fullname());
        Instant instant = Instant.now().plusSeconds(5 *60);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss' UTC'");
        String expirationTime = dateTimeFormatter.format(instant);
        claims.put("exp", String.valueOf(instant.getEpochSecond()));

        String token = jwtHS256Utils.generateJwt(null, claims);

        try {
            mailUtils.sendHtml(
                    email,
                    "test html",
                    "<p>Please confirm before " + expirationTime + "</p> <a href=" + String.format("\"http://%s/api/signup/confirm/%s\"", getDomainAddress(), token) + ">Click link to confirm sign up</a>");
        } catch (AddressException e) {
            return ResponseEntity.unprocessableEntity().body("Invalid email");
        }

        return ResponseEntity.ok(String.format("Go to email %s to confirm", email));
    }

    /**
     * /**
     * New
     * @param token
     * @return Object
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:}
     * curl -v localhost:8000/api/signup/confirm/&lt;token&gt;
     */
    @GetMapping("/api/signup/confirm/{token}")
    ResponseEntity<?> ConfirmEmail(@PathVariable String token) {
        if (!jwtHS256Utils.verifyJwt(token))
            return ResponseEntity.notFound().build();

        Map<String, String> claims = jwtHS256Utils.extractClaims(token);
        if (claims.get("exp") == null || jwtHS256Utils.isExpired(claims.get("exp")))
            return ResponseEntity.notFound().build();

        AccountDTO accountDTO = new AccountDTO(claims.get("password"), claims.get("email"), claims.get("fullname"));

        if (accountDTO.password() == null || accountDTO.password().isBlank())
            return ResponseEntity.unprocessableEntity().build();

        if (accountDTO.fullname() == null || accountDTO.fullname().isBlank())
            return ResponseEntity.unprocessableEntity().build();

        if (accountDTO.email() == null || accountDTO.email().isBlank())
            return ResponseEntity.unprocessableEntity().build();

        String email = accountDTO.email();
        if (repository.existsByEmail(email.trim().toLowerCase()))
            return ResponseEntity.unprocessableEntity().body("Existed Email");

        ResponseEntity<?> response = createUser(accountDTO);

        URI location = response.getHeaders().getLocation();

        if (location == null || response.getStatusCode() != HttpStatus.CREATED)
            return response;

        String locationUrl = location.toString().trim();
        String uid = locationUrl.substring(locationUrl.lastIndexOf("/") + 1);
        if (!Pattern.matches("^[0-9]+$", uid))
            return ResponseEntity.internalServerError().build();

        Long userId = Long.parseLong(uid);
        if (repository.existsByUserId(userId))
            return ResponseEntity.internalServerError().build();

        Account account = new Account();
        account.setPassword(passwordEncoder.encode(accountDTO.password().trim()));
        account.setEmail(accountDTO.email().trim());
        account.setFullname(accountDTO.fullname().trim());
        account.setUserId(userId);

        repository.save(account);

        URI locationUri = URI.create(locationUrl.replaceFirst("://.+/", String.format("://%s/", getDomainAddress())));
        return ResponseEntity.created(locationUri)
                .body(String.format("Sign up success with email %s", accountDTO.email().trim()));
    }

    @PutMapping("/api/edit")
    ResponseEntity<?> update() {
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/change-password")
    ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwords) {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/remove")
    ResponseEntity<?> delete() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Account account = repository.findByEmail(user.getUsername());

        ResponseEntity<?> response = deleteUser(account.getUserId());

        if (response.getStatusCode() != HttpStatus.NO_CONTENT)
            return response;

        repository.delete(account);

        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> createUser(@NonNull AccountDTO accountDTO) throws RestClientException {
        String url = String.format("http://%s/api/user", getDomainAddress());
        System.out.println(url);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-USER-SERVICE-TOKEN", "");
        HttpEntity<?> httpEntity = new HttpEntity<>(accountDTO, httpHeaders);

        try {
            return restTemplate.exchange(url, HttpMethod.POST, httpEntity, Object.class);
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).headers(ex.getResponseHeaders()).body(ex.getResponseBodyAsString());
        }
    }

    private ResponseEntity<?> deleteUser(Long userId) throws RestClientException {
        String url = String.format("http://%s/api/user/%s", getDomainAddress(), userId);
        System.out.println(url);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-USER-ID", "");
        httpHeaders.add("X-USER-SERVICE-TOKEN", "");
        HttpEntity<?> httpEntity = new HttpEntity<>(null, httpHeaders);

        try {
            return restTemplate.exchange(url, HttpMethod.DELETE, httpEntity, Object.class);
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).headers(ex.getResponseHeaders()).body(ex.getResponseBodyAsString());
        }
    }

    private String getDomainAddress() {

        boolean isDevelopmentMode = Boolean.parseBoolean(env.getProperty("development-mode"));

        if (isDevelopmentMode)
            return InetAddress.getLoopbackAddress().getHostAddress() + ":" + env.getProperty("server.port");

        try {
            if (InetAddress.getLocalHost().isLoopbackAddress())
                return InetAddress.getLoopbackAddress().getHostAddress() + ":" + env.getProperty("server.port");

            return InetAddress.getLocalHost().getHostAddress() + ":" + env.getProperty("server.port");

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

}
