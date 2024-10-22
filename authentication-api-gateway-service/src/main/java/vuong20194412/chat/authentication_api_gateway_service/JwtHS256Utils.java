package vuong20194412.chat.authentication_api_gateway_service;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class JwtHS256Utils {

    private final long jwtValidityPeriod;

    private final Mac mac;

    private final Pattern numberPattern = Pattern.compile("^[1-9][0-9]*$");

    JwtHS256Utils(@Value("${JWT_VALIDITY_PERIOD}") long jwtValidityPeriod) throws NoSuchAlgorithmException, InvalidKeyException {
        this.mac = Mac.getInstance("HmacSHA256");
        Map<String, Object> setting = SettingConfig.readSetting();
        String hs256SecretKey = (String) setting.get("base64_hs256_secret_key");
        SecretKey secretKey;
        if (hs256SecretKey == null) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            //keyGenerator.init(256);
            secretKey = keyGenerator.generateKey();

            setting.put("base64_hs256_secret_key", Base64.getEncoder().encodeToString(secretKey.getEncoded()));
            SettingConfig.writeSetting(setting);
        }
        else {
            secretKey = new SecretKeySpec(Base64.getDecoder().decode(hs256SecretKey), "HmacSHA256");
        }
        System.out.println("generateSecretKey " + secretKey.getEncoded().length * 8);
        this.mac.init(secretKey);
        this.jwtValidityPeriod = jwtValidityPeriod;
    }

    public long getJwtValidityPeriod() {
        return jwtValidityPeriod;
    }

    /**
     * Each value can not be null, not contain @code{\"}, not contain @code{any character whose codepoint is less than or equal to 'U+0020'} in leading and trailing, not equal @code{,}
     * @param additionalHeaders Each header can not contain @code{:} and can not be null
     * @param claims iss (issuer), sub (subject), aud (audience), exp (expiration time), nbf (not before), iat (issued at), jti (jwt id), ...
     * @return token
     * @apiNote 0 < exp - iat <= jwtValidityPeriod, now <= iat
     */
    public String generateJwt(Map<String, String> additionalHeaders, Map<String, String> claims) {
        List<String> headers = new ArrayList<>(List.of("\"alg\":\"HS256\"", "\"typ\":\"JWT\""));
        List<String> payloads = new ArrayList<>(List.of("\"_iss\":\"vuong20194412\""));

        if (additionalHeaders != null) {
            additionalHeaders.remove("alg");
            additionalHeaders.remove("typ");

            for (Map.Entry<String, String> header : additionalHeaders.entrySet()) {
                if (isValidEntry(header.getKey(), header.getValue()))
                    headers.add(String.format("\"%s\":\"%s\"", header.getKey(), header.getValue()));
            }
        }

        if (claims != null) {
            claims.remove("_iss");

            long now = Instant.now().getEpochSecond();

            long iat;
            String claimIat = claims.getOrDefault("iat", String.valueOf(now));
            if (claimIat == null || !numberPattern.matcher(claimIat).matches() || Long.parseLong(claimIat) < now) {
                claims.remove("iat");
                iat = now;
            }
            else {
                iat = Long.parseLong(claimIat);
            }
            payloads.add(String.format("\"iat\":\"%s\"", iat));

            long exp;
            String claimExp = claims.getOrDefault("exp", String.valueOf(iat + jwtValidityPeriod));
            if (claimExp == null || !numberPattern.matcher(claimExp).matches() || Long.parseLong(claimExp) <= iat || Long.parseLong(claimExp) > iat + jwtValidityPeriod) {
                claims.remove("exp");
                exp = iat + jwtValidityPeriod;
            }
            else {
                exp = Long.parseLong(claimExp);
            }
            payloads.add(String.format("\"exp\":\"%s\"", exp));

            for (Map.Entry<String, String> claim : claims.entrySet()) {
                if (isValidEntry(claim.getKey(), claim.getValue()))
                    payloads.add(String.format("\"%s\":\"%s\"", claim.getKey(), claim.getValue()));
            }
        }

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(String.format("{%s}", String.join(",", headers)).getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(String.format("{%s}", String.join(",", payloads)).getBytes(StandardCharsets.UTF_8));

        String encodedRaw = String.format("%s.%s", encodedHeader, encodedPayload);
        String signature = createSignature(encodedRaw);

        return String.format("%s.%s", encodedRaw, signature);
    }

    /**
     * @param key nullable
     * @param value nullable
     * @return true if key and value do not contain @code{\"}, do not equal null or @code{,}, do not contain @code{codepoint <= 32} in leading and trailing, and key do not contain @code{:}. Otherwise, return false
     */
    private boolean isValidEntry(String key, String value) {
        if (key == null || value == null)
            return false;

        String trimmedKey = key.trim();
        String trimmedValue = value.trim();

        return !trimmedKey.equals(",") && !trimmedValue.equals(",") && key.equals(trimmedKey) && value.equals(trimmedValue) && !trimmedKey.contains(":");
    }

    private String createSignature(@NotNull String encodedRaw) {
        byte[] signature = mac.doFinal(encodedRaw.getBytes(StandardCharsets.UTF_8));

        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    public boolean verifyJwt(@NotNull String jwt) {
        Map<String, String> headers = extractHeaders(jwt);
        if (headers == null)
            return false;
        if (!"HS256".equals(headers.get("alg")))
            return false;
        if (!"JWT".equals(headers.get("typ")))
            return false;
        if ("\"st".equals(headers.get("t\\\"e"))) {
            System.out.println(1);
        }
        if ("\\\"st".equals(headers.get("t\\\"e"))) {
            System.out.println(2);
        }
        if ("\"st".equals(headers.get("t\"e"))) {
            System.out.println(3);
        }
        if ("\\\"st".equals(headers.get("t\"e"))) {
            System.out.println(4);
        }

        String[] parts = jwt.split("\\.");

        String encodedRaw = String.format("%s.%s", parts[0], parts[1]);
        String signature = parts[2];

        return createSignature(encodedRaw).equals(signature);
    }

    public Map<String, String> extractClaims(@NotNull String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3)
            return null;

        String encodedPayload = parts[1];

        String decodedPayload = new String(Base64.getUrlDecoder().decode(encodedPayload));

        if (!decodedPayload.startsWith("{") || !Pattern.matches("^(\".*\":\".*\")(,\".*\":\".*\")*}$", decodedPayload.substring(1)))
            return null;

        String[] decodedClaims = decodedPayload.substring(1, decodedPayload.length() - 1).split(",");

        Map<String, String> claims = new HashMap<>();

        for (String claim: decodedClaims) {
            // "\"key\":\"value\""
            String[] entry = claim.substring(1, claim.length() - 1).split("\":\"", 2);
            if (entry.length == 2)
                claims.put(entry[0], entry[1]);
        }

        return claims;
    }

    Map<String, String> extractHeaders(@NotNull String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3)
            return null;

        String encodedHeader = parts[0];

        String decodedHeader = new String(Base64.getUrlDecoder().decode(encodedHeader));

        if (!decodedHeader.startsWith("{") || !Pattern.matches("^(\".*\":\".*\")(,\".*\":\".*\")*}$", decodedHeader.substring(1)))
            return null;

        String[] decodedHeaders = decodedHeader.substring(1, decodedHeader.length() - 1).split(",");

        Map<String, String> headers = new HashMap<>();

        for (String header: decodedHeaders) {
            // "\"key\":\"value\""
            String[] entry = header.substring(1, header.length() - 1).split("\":\"", 2);
            if (entry.length == 2)
                headers.put(entry[0], entry[1]);
        }

        return headers;
    }

    public boolean isExpired(String exp) {
        if (!numberPattern.matcher(exp).matches())
            return true;

        return Long.parseLong(exp) < Instant.now().getEpochSecond();
    }

}
