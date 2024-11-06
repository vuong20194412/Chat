package vuong20194412.chat.authentication_api_gateway_service.util;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
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
public class JwtHS256Util {

    private final long jwtValidityPeriod;

    private final Mac mac;

    private final Pattern numberPattern = Pattern.compile("^[1-9][0-9]*$");

    JwtHS256Util(@Value("${JWT_VALIDITY_PERIOD}") long jwtValidityPeriod, @Autowired SettingUtil settingUtil) throws NoSuchAlgorithmException, InvalidKeyException {
        this.mac = Mac.getInstance("HmacSHA256");
        Map<String, Object> setting = settingUtil.readSetting();
        String hs256SecretKey = (String) setting.get("base64_hs256_secret_key");
        SecretKey secretKey;
        if (hs256SecretKey == null) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            //keyGenerator.init(256);
            secretKey = keyGenerator.generateKey();

            setting.put("base64_hs256_secret_key", Base64.getEncoder().encodeToString(secretKey.getEncoded()));
            settingUtil.writeSetting(setting);
        }
        else {
            secretKey = new SecretKeySpec(Base64.getDecoder().decode(hs256SecretKey), "HmacSHA256");
        }
        System.out.println("generateSecretKey " + secretKey.getEncoded().length * 8);
        this.mac.init(secretKey);
        this.jwtValidityPeriod = jwtValidityPeriod;
    }

    /**
     * Each value can not be null, not contain @code{\"}, not contain @code{any character whose codepoint is less than or equal to 'U+0020'} in leading and trailing, not equal @code{,}
     * @param additionalHeaders Each header can not contain @code{:} and can not be null
     * @param additionalClaims iss (issuer), sub (subject), aud (audience), exp (expiration time), nbf (not before), iat (issued at), jti (jwt id), ...
     * @return {"token": {"encodedToken": encodedToken, "encodedSignature": encodedSignature}, "headers": headers (Map&lt;String,String&gt;), "claims": claims (Map&lt;String,String&gt;)}
     * @apiNote 0 < exp - iat <= jwtValidityPeriod, now <= iat
     */
    public Map<String, Map<String, String>> generateJwt(Map<String, String> additionalHeaders, Map<String, String> additionalClaims) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("alg", "HS256");
        headers.put("typ", "JWT");

        Map<String, String> claims = new HashMap<>();
        claims.put("_iss", "vuong20194412");

        if (additionalHeaders != null) {
            additionalHeaders.remove("alg");
            additionalHeaders.remove("typ");

            for (Map.Entry<String, String> entry : additionalHeaders.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (isValidEntry(key, value))
                    headers.put(key, value);
            }
        }

        if (additionalClaims != null) {
            additionalClaims.remove("_iss");

            for (Map.Entry<String, String> entry : additionalClaims.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (isValidEntry(key, value))
                    claims.put(key, value);
            }

            long now = Instant.now().getEpochSecond();

            String claimIat = claims.getOrDefault("iat", "");
            long iat = (!numberPattern.matcher(claimIat).matches() || Long.parseLong(claimIat) < now) ? now : Long.parseLong(claimIat);
            claims.put("iat", String.valueOf(iat));

            long maxExp = now + jwtValidityPeriod;

            String claimExp = claims.getOrDefault("exp", "");
            long exp = (!numberPattern.matcher(claimExp).matches() || Long.parseLong(claimExp) <= iat || Long.parseLong(claimExp) > maxExp) ? maxExp : Long.parseLong(claimExp);
            claims.put("exp", String.valueOf(exp));
        }

        String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(mapToJsonString(headers).getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(mapToJsonString(claims).getBytes(StandardCharsets.UTF_8));

        String encodedRaw = String.format("%s.%s", encodedHeader, encodedPayload);
        String signature = createSignature(encodedRaw);

        String token = String.format("%s.%s", encodedRaw, signature);

        return Map.of("token", Map.of("encodedToken", token, "encodedSignature", signature), "headers", headers, "claims", claims);
    }

    private String mapToJsonString(@NonNull Map<String,String> map) {
        return "{%s}".formatted(String.join(",", map.entrySet()
                .stream()
                .map(entry -> String.format("\"%s\":\"%s\"", entry.getKey(), entry.getValue()))
                .toList()));
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

    public boolean isNotIssued(String iat) {
        if (!numberPattern.matcher(iat).matches())
            return true;

        return Long.parseLong(iat) >= Instant.now().getEpochSecond();
    }
}
