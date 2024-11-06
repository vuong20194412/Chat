package vuong20194412.chat.authentication_api_gateway_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import vuong20194412.chat.authentication_api_gateway_service.util.DomainAddressUtil;
import vuong20194412.chat.authentication_api_gateway_service.util.MailUtil;
import vuong20194412.chat.authentication_api_gateway_service.dto.EntityDTO;
import vuong20194412.chat.authentication_api_gateway_service.dto.PasswordDTO;
import vuong20194412.chat.authentication_api_gateway_service.entity.TransientPassword;
import vuong20194412.chat.authentication_api_gateway_service.exception.AccountInternalServerErrorException;
import vuong20194412.chat.authentication_api_gateway_service.exception.AccountUnprocessableEntityException;
import vuong20194412.chat.authentication_api_gateway_service.entity.Account;
import vuong20194412.chat.authentication_api_gateway_service.repository.AccountRepository;
import vuong20194412.chat.authentication_api_gateway_service.repository.TransientPasswordRepository;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Service
public class AccountService {

    private final TransientPasswordRepository passwordRepository;

    private final AccountRepository repository;

    private final MailUtil mailUtils;

    private final PasswordEncoder passwordEncoder;

    private final RestTemplate restTemplate;

    private final String domainAddress;

    @Autowired
    public AccountService(TransientPasswordRepository passwordRepository, AccountRepository repository, MailUtil mailUtils, PasswordEncoder passwordEncoder, RestTemplate restTemplate, DomainAddressUtil domainAddressConfig) {
        this.passwordRepository = passwordRepository;
        this.repository = repository;
        this.mailUtils = mailUtils;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
        this.domainAddress = domainAddressConfig.getDomainAddress();
    }

    public AccountRepository getRepository() {
        return repository;
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public Account.AccountRecord prepareBeforeCreateAccount(String rawPassword, String email, String fullname) {
        Map<AccountUnprocessableEntityException.Type, List<String>> errors = new HashMap<>();
        String _fullname = fullname != null ? fullname.trim() : "";
        if (_fullname.isBlank()) {
            errors.put(AccountUnprocessableEntityException.Type.MISSING_FIELD, new ArrayList<>());
            errors.get(AccountUnprocessableEntityException.Type.MISSING_FIELD).add("fullname");
        }

        String _email = email != null ? email.trim().toLowerCase() : "";
        if (_email.isBlank()) {
            if (!errors.containsKey(AccountUnprocessableEntityException.Type.MISSING_FIELD))
                errors.put(AccountUnprocessableEntityException.Type.MISSING_FIELD, new ArrayList<>());
            errors.get(AccountUnprocessableEntityException.Type.MISSING_FIELD).add("email");
        }
        else if (!mailUtils.isValidEmailAddress(_email)) {
            errors.put(AccountUnprocessableEntityException.Type.INVALID_EMAIL, new ArrayList<>());
            errors.get(AccountUnprocessableEntityException.Type.INVALID_EMAIL).add("email");
        }
        else if (repository.existsByEmailOrGmailOrOutlook(_email, _email, _email)) {
            errors.put(AccountUnprocessableEntityException.Type.EXISTED_EMAIL, new ArrayList<>());
            errors.get(AccountUnprocessableEntityException.Type.EXISTED_EMAIL).add("email");
        }

        String _rawPassword = rawPassword != null ? rawPassword.trim() : "";
        if (_rawPassword.isBlank()) {
            if (!errors.containsKey(AccountUnprocessableEntityException.Type.MISSING_FIELD))
                errors.put(AccountUnprocessableEntityException.Type.MISSING_FIELD, new ArrayList<>());
            errors.get(AccountUnprocessableEntityException.Type.MISSING_FIELD).add("password");
        }

        if (!errors.isEmpty())
            throw new AccountUnprocessableEntityException(errors);

        return Account.AccountRecord.createSimpleRecord(_rawPassword, _email, _fullname);
    }

    public TransientPassword saveTransientPassword(String rawPassword, Long erasableTime) {
        TransientPassword transientPassword = new TransientPassword();
        transientPassword.setEncodedNewPassword(passwordEncoder.encode(rawPassword));
        transientPassword.setErasableTime(erasableTime);
        return passwordRepository.save(transientPassword);
    }

    /**
     * Create but do not save
     */
    public Account createAccount(String rawPassword, String lowerCaseEmail, String fullname) {
        String encodedPassword = passwordEncoder.encode(rawPassword);

        Account account = new Account();
        account.setPassword(encodedPassword);
        account.setEmail(lowerCaseEmail);
        account.setFullname(fullname);

        return account;
    }

    public TransientPassword getTransientPassword(Long passwordId, String randomCode) {
        return passwordRepository.findByIdAndRandomCodeAndErasableTimeGreaterThan(passwordId, randomCode, Instant.now().getEpochSecond());
    }

    /**
     * Create but do not save
     */
    public Account createAccountWithEncodePassword(TransientPassword transientPassword, String lowerCaseEmail, String fullname) {
        if (transientPassword == null)
            throw new AccountInternalServerErrorException("Please try again.");

        Account account = new Account();
        account.setPassword(transientPassword.getEncodedNewPassword());
        account.setEmail(lowerCaseEmail);
        account.setFullname(fullname);

        return account;
    }

    public PasswordDTO prepareBeforeChangePassword(PasswordDTO passwordDTO) {
        Map<AccountUnprocessableEntityException.Type, List<String>> errors = new HashMap<>();

        String _rawPassword = passwordDTO.rawPassword() != null ? passwordDTO.rawPassword().trim() : "";
        String _newRawPassword = passwordDTO.rawNewPassword() != null ? passwordDTO.rawNewPassword().trim() : "";
        String _repeatedNewRawPassword = passwordDTO.rawRepeatedNewPassword() != null ? passwordDTO.rawRepeatedNewPassword().trim() : "";

        if (_rawPassword.isBlank()) {
            errors.put(AccountUnprocessableEntityException.Type.MISSING_FIELD, new ArrayList<>());
            errors.get(AccountUnprocessableEntityException.Type.MISSING_FIELD).add("oldPassword");
        }

        if (_newRawPassword.isBlank()) {
            if (errors.containsKey(AccountUnprocessableEntityException.Type.MISSING_FIELD))
                errors.put(AccountUnprocessableEntityException.Type.MISSING_FIELD, new ArrayList<>());
            errors.get(AccountUnprocessableEntityException.Type.MISSING_FIELD).add("newPassword");
        }

        if (!_newRawPassword.equals(_repeatedNewRawPassword)) {
            errors.put(AccountUnprocessableEntityException.Type.NO_MATCH_PASSWORDS, new ArrayList<>());
            errors.get(AccountUnprocessableEntityException.Type.NO_MATCH_PASSWORDS).add("newPassword-repeatedNewPassword");
        }

        if (_newRawPassword.equals(_rawPassword)) {
            errors.put(AccountUnprocessableEntityException.Type.NO_CHANGE, new ArrayList<>());
            errors.get(AccountUnprocessableEntityException.Type.NO_CHANGE).add("newPassword-OldPassword");
        }

        if (!errors.isEmpty())
            throw new AccountUnprocessableEntityException(errors);

        return new PasswordDTO(_rawPassword, _newRawPassword, _repeatedNewRawPassword);
    }

    public Account changePasswordWithEncodePassword(TransientPassword transientPassword, String email) {
        if (transientPassword == null)
            throw new AccountInternalServerErrorException("Please try again.");

        Account account = repository.findByEmail(email);
        account.setPassword(transientPassword.getEncodedNewPassword());
        return repository.save(account);
    }

    public String prepareBeforeChangeEmail(String oldEmail, String newEmail) {
        String _newEmail = newEmail != null ? newEmail.trim().toLowerCase() : "";
        if (_newEmail.isBlank())
            throw new AccountUnprocessableEntityException(AccountUnprocessableEntityException.Type.MISSING_FIELD, "newEmail");
        else if (!mailUtils.isValidEmailAddress(_newEmail)) {
            throw new AccountUnprocessableEntityException(AccountUnprocessableEntityException.Type.INVALID_EMAIL, "newEmail");
        }
        else if (!_newEmail.equals(oldEmail) && repository.existsByEmailOrGmailOrOutlook(_newEmail, _newEmail, _newEmail)) {
            throw new AccountUnprocessableEntityException(AccountUnprocessableEntityException.Type.EXISTED_EMAIL, "newEmail");
        }

        return _newEmail;
    }

    public Account saveAccount(Account account) {
        return repository.save(account);
    }

    public ResponseEntity<EntityDTO> createUser(String email, String fullname) {
        String url = String.format("http://%s/api/user", domainAddress);
        System.out.println(url);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("fullname", fullname);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-USER-SERVICE-TOKEN", "");
        HttpEntity<?> httpEntity = new HttpEntity<>(body, httpHeaders);

        try {

            ResponseEntity<EntityDTO> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, EntityDTO.class);

            if (response.getStatusCode() != HttpStatus.CREATED)
                throw new RuntimeException(ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(response.getBody()).toString());

            if (response.getBody() == null) {
                URI location = response.getHeaders().getLocation();

                if (location == null)
                    throw new RuntimeException(ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(response.getBody()).toString());

                String locationUrl = location.toString().trim();
                String uid = locationUrl.substring(locationUrl.lastIndexOf("/") + 1);
                if (!Pattern.matches("^[1-9][0-9]*$", uid))
                    throw new RuntimeException(ResponseEntity.internalServerError().build().toString());

                Long userId = Long.parseLong(uid);
                if (repository.existsByUserId(userId))
                    throw new RuntimeException(ResponseEntity.internalServerError().build().toString());

                EntityDTO entityDTO = new EntityDTO();
                entityDTO.put("id", userId);

                return ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(entityDTO);
            }

            Object oUserId = response.getBody().get("id");
            if (oUserId != null) {
                Long userId = Long.parseLong(String.valueOf(oUserId));
                if (repository.existsByUserId(userId))
                    throw new RuntimeException(ResponseEntity.internalServerError().build().toString());

                response.getBody().replace("id", userId);
            }
            else {
                URI location = response.getHeaders().getLocation();

                if (location == null)
                    throw new RuntimeException(ResponseEntity.status(response.getStatusCode()).headers(response.getHeaders()).body(response.getBody()).toString());

                String locationUrl = location.toString().trim();
                String uid = locationUrl.substring(locationUrl.lastIndexOf("/") + 1);
                if (!Pattern.matches("^[1-9][0-9]*$", uid))
                    throw new RuntimeException(ResponseEntity.internalServerError().build().toString());

                Long userId = Long.parseLong(uid);
                if (repository.existsByUserId(userId))
                    throw new RuntimeException(ResponseEntity.internalServerError().build().toString());

                response.getBody().put("id", userId);
            }

            return response;

        } catch (RestClientResponseException ex) {
            throw new RuntimeException(ResponseEntity.status(ex.getStatusCode()).headers(ex.getResponseHeaders()).body(ex.getResponseBodyAsString()).toString());
        }
    }

    public ResponseEntity<Object> updateUser(Long userId, Map<String, Object> body) throws RestClientException {
        String url = String.format("http://%s/api/user/%s", domainAddress, userId);
        System.out.println(url);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-USER-ID", String.valueOf(userId));
        httpHeaders.add("X-USER-SERVICE-TOKEN", "");
        HttpEntity<?> httpEntity = new HttpEntity<>(body, httpHeaders);

        try {

            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.PUT, httpEntity, Object.class);

            if (response.getStatusCode() != HttpStatus.ACCEPTED)
                throw new RuntimeException(response.toString());

            return response;

        } catch (RestClientResponseException ex) {
            throw new RuntimeException(ResponseEntity.status(ex.getStatusCode()).headers(ex.getResponseHeaders()).body(ex.getResponseBodyAsString()).toString());
        }
    }

    public void deleteUser(Long userId) throws RestClientException {
        String url = String.format("http://%s/api/user/%s", domainAddress, userId);
        System.out.println(url);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("X-USER-ID", "");
        httpHeaders.add("X-USER-SERVICE-TOKEN", "");
        HttpEntity<?> httpEntity = new HttpEntity<>(null, httpHeaders);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.DELETE, httpEntity, Object.class);

            if (response.getStatusCode() != HttpStatus.NO_CONTENT)
                throw new RuntimeException(response.toString());

        } catch (RestClientResponseException ex) {
            throw new RuntimeException(ResponseEntity.status(ex.getStatusCode()).headers(ex.getResponseHeaders()).body(ex.getResponseBodyAsString()).toString());
        }
    }

    public String generatePassword() {
        Random random = new Random();
        char[] chars = new char[4];
        return String.join("", IntStream.range(0, 2).mapToObj(i -> {
            chars[3] = (char) switch (random.nextInt(0, 3)) {
               case 1 -> random.nextInt(58, 65);
               case 2 -> (random.nextInt(91, 97) + random.nextInt(123, 127))/2;
               default -> random.nextInt(33, 48);
            }; // symbol
            chars[2] = (char) random.nextInt(48, 58); // number
            chars[1] = (char) random.nextInt(65, 91); // upper
            chars[0] = (char) random.nextInt(97, 123); // lower
            return switch (random.nextInt(0, 4)) {
                case 1 -> String.valueOf(chars[0]) + chars[1] + chars[2] + chars[3];
                case 2 -> String.valueOf(chars[1]) + (chars[0] + chars[2])/2 + chars[0] + chars[3];
                case 3 -> String.valueOf(chars[0]) + (chars[1] + chars[3])/2 + chars[1] + chars[2];
                default -> String.valueOf(chars[1]) + chars[0] + chars[3] + chars[2];
            };
        }).toList());
    }
}
