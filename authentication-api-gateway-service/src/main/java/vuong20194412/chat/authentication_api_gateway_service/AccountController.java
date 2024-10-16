package vuong20194412.chat.authentication_api_gateway_service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
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

        if (response == null)
            return response;

        URI location = response.getHeaders().getLocation();

        if (location == null || response.getStatusCode() == HttpStatus.CREATED)
            return response;

        String locationUrl = location.toString().trim();
        String uid = locationUrl.substring(locationUrl.lastIndexOf("/") + 1);
        if (!Pattern.matches("^[0-9]+$", uid))
            return ResponseEntity.internalServerError().build();

        Long userId = Long.parseLong(uid);
        if (repository.existsByUserIdOrEmail(userId, accountDTO.email().trim()))
            return ResponseEntity.internalServerError().build();

        Account account = new Account();
        account.setPassword(passwordEncoder.encode(account.getPassword().trim()));
        account.setEmail(account.getEmail().trim());
        account.setFullname(account.getFullname().trim());
        account.setUserId(userId);

        System.out.println(repository.save(account).getPassword());

        return ResponseEntity.created(location).body("Sign up success");
    }

//    @PostMapping({"/api/logout", "/api/logout/"})
//    ResponseEntity<?> logOut(HttpServletRequest request, HttpServletResponse response) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication != null) {
//            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
//            logoutHandler.logout(request, response, authentication);
//        }
//        return ResponseEntity.ok().body("Log out success");
//    }

    private ResponseEntity<?> createUser(@NonNull AccountDTO accountDTO) throws RestClientException {
        //String url = String.format("%s/api/user", env.getProperty("server-domain"));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-USER-SERVICE-TOKEN", "");
        HttpEntity<?> httpEntity = new HttpEntity<>(accountDTO, httpHeaders);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(URI.create("http://localhost:8100/api/user"), HttpMethod.POST, httpEntity, Object.class);
            return response;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

}
