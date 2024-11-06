package vuong20194412.chat.authentication_api_gateway_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import vuong20194412.chat.authentication_api_gateway_service.util.JwtHS256Util;
import vuong20194412.chat.authentication_api_gateway_service.entity.BlackJsonWebTokenHash;
import vuong20194412.chat.authentication_api_gateway_service.exception.JwtErrorException;
import vuong20194412.chat.authentication_api_gateway_service.repository.BlackJsonWebTokenHashRepository;

import java.time.Instant;
import java.util.Map;

@Service
public class JwtService {

    private final JwtHS256Util jwtHS256Utils;

    private final BlackJsonWebTokenHashRepository blackJwtHashRepository;

    @Autowired
    public JwtService(JwtHS256Util jwtHS256Utils, BlackJsonWebTokenHashRepository blackJwtHashRepository) {
        this.jwtHS256Utils = jwtHS256Utils;
        this.blackJwtHashRepository = blackJwtHashRepository;
    }

    /**
     * Create token with token not in token hash not in blacklist
     * @param claims not contain key _salt
     * @return token
     * @throws JwtErrorException when can not generate jwt
     */
    public String generateJwt(Map<String, String> claims) throws JwtErrorException {
        if (claims.containsKey("_salt"))
            throw new JwtErrorException("Existed key _salt");

        claims.put("_salt", String.valueOf(Instant.now().getEpochSecond()));
        String base64UrlEncodedToken = generateJwtOneTime(claims, true);
        if (base64UrlEncodedToken != null)
            return base64UrlEncodedToken;

        claims.put("_salt", String.valueOf(Instant.now().getEpochSecond()));
        String retriedBase64UrlEncodedToken = generateJwtOneTime(claims, true);
        if (retriedBase64UrlEncodedToken != null)
            return retriedBase64UrlEncodedToken;

        throw new JwtErrorException("Can not generate json web token");
    }

    public boolean isBlackToken(String encodedSignature, Long issuedAtTime, Long expirationTime) {
        return blackJwtHashRepository.existsByEncodedHashAndIssuedAtTimeAndExpirationTime(encodedSignature, issuedAtTime, expirationTime);
    }

    public String generateJwtOneTime(@NonNull Map<String, String> claims) {
        return generateJwtOneTime(claims, false);
    }

    private String generateJwtOneTime(@NonNull Map<String, String> claims, boolean isNotInBlackTokens) {
        Map<String, Map<String, String>> result = jwtHS256Utils.generateJwt(null, claims);
        Map<String, String> token = result.get("token");
        String base64UrlEncodedToken = token.get("encodedToken");
        if (!isNotInBlackTokens)
            return base64UrlEncodedToken;

        String base64UrlEncodedTokenHash = token.get("encodedSignature");
        Map<String, String> _claims = result.get("claims");
        Long issuedAtTime = Long.parseLong(_claims.get("iat"));
        Long expirationTime = Long.parseLong(_claims.get("exp"));
        return !isBlackToken(base64UrlEncodedTokenHash, issuedAtTime, expirationTime) ? base64UrlEncodedToken : null;
    }

    public Map<String, String> extractClaims(@NonNull String token) throws JwtErrorException {
        if (!jwtHS256Utils.verifyJwt(token))
            throw new JwtErrorException("Invalid token");

        return extractClaimsFromValidToken(token);
    }

    private Map<String, String> extractClaimsFromValidToken(@NonNull String token)  {
        Map<String, String> claims = jwtHS256Utils.extractClaims(token);
        if (claims == null)
            throw new JwtErrorException("Invalid token");

        if (claims.get("iat") == null || jwtHS256Utils.isNotIssued(claims.get("iat")))
            throw new JwtErrorException("Invalid issued at time");

        if (claims.get("exp") == null || jwtHS256Utils.isExpired(claims.get("exp")))
            throw new JwtErrorException("Invalid expiration time");

        return claims;
    }

    public BlackJsonWebTokenHash saveBlackJsonWebTokenHash(@NonNull String token) {
        try {
            Map<String, String> claims = extractClaimsFromValidToken(token);
            String encodedSignature = token.split("\\.")[2];

            BlackJsonWebTokenHash blackJsonWebTokenHash = new BlackJsonWebTokenHash();

            blackJsonWebTokenHash.setEncodedHash(encodedSignature);
            blackJsonWebTokenHash.setIssuedAtTime(Long.parseLong(claims.get("iat")));
            blackJsonWebTokenHash.setExpirationTime(Long.parseLong(claims.get("exp")));

            return blackJwtHashRepository.save(blackJsonWebTokenHash);
        } catch (JwtErrorException ex) {
            return null;
        }
    }
}
