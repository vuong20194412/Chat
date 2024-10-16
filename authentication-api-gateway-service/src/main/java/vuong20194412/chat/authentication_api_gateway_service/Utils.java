package vuong20194412.chat.authentication_api_gateway_service;

import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Component
class JwtHS256Utils {

    private final SecretKey secretKey = generateSecretKey();

    private final Pattern numberPattern = Pattern.compile("^[1-9][0-9]*$");

    @NotNull
    private SecretKey generateSecretKey() {
        Random random = new Random();
        char[] randomKey = new char[16];
        IntStream.range(0, 16).forEach(i -> {
            switch (random.nextInt(3)) {
                case 0: {
                    randomKey[i] = (char) random.nextInt('a', 'z' + 1);
                }
                case 1: {
                    randomKey[i] = (char) random.nextInt('A', 'Z' + 1);
                }
                default: {
                    randomKey[i] = (char) random.nextInt('0', '9' + 1);
                }
            }
        });
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            //keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            System.out.println(Arrays.toString(secretKey.getEncoded()));
            System.out.println(Arrays.toString(randomKey));
            return secretKey;
        } catch (NoSuchAlgorithmException e) {
            return new SecretKeySpec(Arrays.toString(randomKey).getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        }
    }

    /**
     * Each value can not be null
     * @param additionalHeaders Each header can not contain ':' and can not be null
     * @param claims iss (issuer), sub (subject), aud (audience), exp (expiration time), nbf (not before), iat (issued at), jti (jwt id), ...
     * @return token
     */
    String generateJwt(Map<String, String> additionalHeaders, Map<String, String> claims) {
        List<String> headers = Arrays.asList("\"alg\":\"HS256\"", "\"typ\":\"JWT\"");
        List<String> payloads = new ArrayList<>();
        if (additionalHeaders != null) {
            additionalHeaders.remove("alg");
            additionalHeaders.remove("typ");

            for (Map.Entry<String, String> header : additionalHeaders.entrySet()) {
                if (header.getKey() != null && !header.getKey().contains(":") && header.getValue() != null)
                    headers.add(String.format("\"%s\":\"%s\"",
                            header.getKey().trim().replace("\"", "\\\""),
                            header.getValue().trim().replace("\"", "\\\"")));
            }
        }
        if (claims != null) {
            long validityPeriod = 3600 * 24 * 7;
            if (!claims.containsKey("exp"))
                claims.put("exp", String.valueOf(Instant.now().getEpochSecond() + validityPeriod));
            else if (claims.get("exp") == null || !numberPattern.matcher(claims.get("exp")).matches())
                claims.replace("exp", String.valueOf(Instant.now().getEpochSecond() + validityPeriod));

            if (!claims.containsKey("iat"))
                claims.put("iat", String.valueOf(Instant.now()));
            else if (claims.get("iat") == null || !numberPattern.matcher(claims.get("iat")).matches())
                claims.replace("iat", String.valueOf(Instant.now()));

            for (Map.Entry<String, String> claim : claims.entrySet()) {
                if (claim.getKey() != null && !claim.getKey().contains(":") && claim.getValue() != null)
                    payloads.add(String.format("\"%s\":\"%s\"",
                            claim.getKey().trim().replace("\"", "\\\""),
                            claim.getValue().trim().replace("\"", "\\\"")));
            }
        }

        System.out.println(headers);
        if (!payloads.isEmpty())
            System.out.println(payloads.get(0));

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(String.join(",", headers).getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(String.join(",", payloads).getBytes(StandardCharsets.UTF_8));

        String encodedRaw = String.format("%s.%s", encodedHeader, encodedPayload);
        String signature = createSignature(encodedRaw);

        return String.format("%s.%s", encodedRaw, signature);
    }

    private String createSignature(@NotNull String encodedRaw) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);

            byte[] signature = mac.doFinal(encodedRaw.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    boolean verifyJwt(@NotNull String jwt) {
        Map<String, String> headers = extractHeaders(jwt);
        if (headers == null)
            return false;
        if (!"HS256".equals(headers.get("alg")))
            return false;
        if (!"JWT".equals(headers.get("typ")))
            return false;

        String[] parts = jwt.split("\\.");

        String encodedRaw = String.format("%s.%s", parts[0], parts[1]);
        String signature = parts[2];

        return createSignature(encodedRaw).equals(signature);
    }

    Map<String, String> extractClaims(@NotNull String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3)
            return null;

        String encodedPayload = parts[1];

        String[] decodedPayload = new String(Base64.getUrlDecoder().decode(encodedPayload)).split(",");

        Map<String, String> claims = new HashMap<>();

        for (String claim: decodedPayload) {
            String[] entry = claim.split(",");
            if (entry.length != 2)
                continue;
            claims.put(entry[0], entry[1]);
        }

        return claims;
    }

    Map<String, String> extractHeaders(@NotNull String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3)
            return null;

        String encodedHeader = parts[0];

        String[] decodedHeader = new String(Base64.getUrlDecoder().decode(encodedHeader)).split(",");

        Map<String, String> headers = new HashMap<>();

        for (String header: decodedHeader) {
            String[] entry = header.split(",");
            if (entry.length != 2)
                continue;
            headers.put(entry[0], entry[1]);
        }

        return headers;
    }

    boolean isExpired(String exp) {
        if (!numberPattern.matcher(exp).matches())
            return true;

        return Long.parseLong(exp) > Instant.now().getEpochSecond();
    }

}
