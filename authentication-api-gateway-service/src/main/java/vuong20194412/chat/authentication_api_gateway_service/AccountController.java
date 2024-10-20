package vuong20194412.chat.authentication_api_gateway_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.*;
import vuong20194412.chat.authentication_api_gateway_service.model.Account;
import vuong20194412.chat.authentication_api_gateway_service.model.AccountDTO;
import vuong20194412.chat.authentication_api_gateway_service.repository.AccountRepository;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

@RestController
class AccountController {

    @Autowired
    private Environment env;

    private final RestTemplate restTemplate;

    private final PasswordEncoder passwordEncoder;

    private final AccountRepository repository;

    @Autowired
    public AccountController(RestTemplate restTemplate, PasswordEncoder passwordEncoder, AccountRepository repository) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.passwordEncoder = passwordEncoder;
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
     * New
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

        ResponseEntity<?> response = createUser(accountDTO);

        URI location = response.getHeaders().getLocation();

        if (location == null || response.getStatusCode() != HttpStatus.CREATED)
            return response;

        String locationUrl = location.toString().trim();
        String uid = locationUrl.substring(locationUrl.lastIndexOf("/") + 1);
        if (!Pattern.matches("^[0-9]+$", uid))
            return ResponseEntity.internalServerError().build();

        Long userId = Long.parseLong(uid);
        if (repository.existsByUserIdOrEmail(userId, accountDTO.email().trim()))
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
