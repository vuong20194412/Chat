package vuong20194412.chat.authentication_api_gateway_service.security;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vuong20194412.chat.authentication_api_gateway_service.exception.JwtErrorException;
import vuong20194412.chat.authentication_api_gateway_service.entity.Account;
import vuong20194412.chat.authentication_api_gateway_service.dto.SignupDTO;
import vuong20194412.chat.authentication_api_gateway_service.repository.AccountRepository;
import vuong20194412.chat.authentication_api_gateway_service.service.JwtService;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final UserDetailsService userDetailsService;

    @Autowired
    JwtAuthorizationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    public JwtService getJwtService() {
        return jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = SecurityContextHolder.getContext().getAuthentication() == null ? extractToken(request) : null;

        if (token != null) {
            Map<String, String> claims;

            try {
                claims = jwtService.extractClaims(token);
            } catch (JwtErrorException ex) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Expired or invalid token");
                return;
            }

            String email = claims.get("sub");
            if (email == null || email.isBlank() || email.contains("\n")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unprocessable email");
                return;
            }

            if (jwtService.isBlackToken(token.split("\\.")[2], Long.parseLong(claims.get("iat")), Long.parseLong(claims.get("exp")))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Blacklisted or Expired or invalid token");
                return;
            }

            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(email.trim().toLowerCase());
            } catch (UsernameNotFoundException ex) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not Found user, or Blacklisted or Expired or invalid token");
                return;
            }

            if (userDetails == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Blacklisted or Expired or invalid token, or Not Found user");
                return;
            }

            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not enabled or Locked user");
                return;
            }

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        filterChain.doFilter(request, response);
    }

    protected String extractToken(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer "))
            return null;

        return authorizationHeader.split(" ")[1];
    }

}

@Component
class JwtAuthenticationFilter extends OncePerRequestFilter {

    private AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final AccountRepository accountRepository;

    @Value("${JWT_VALIDITY_PERIOD}")
    private Long jwtValidityPeriod;

    @Autowired
    JwtAuthenticationFilter(JwtService jwtService, AccountRepository accountRepository) {
        this.jwtService = jwtService;
        this.accountRepository = accountRepository;
    }

    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (matchRequest(request)) {
            SignupDTO accountDTO;

            try {
                accountDTO = new ObjectMapper().readValue(request.getInputStream(), SignupDTO.class);
            }
            catch (StreamReadException | DatabindException ex) {
                System.out.println(ex.getMessage());
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON data");
                return;
            }
            catch (IOException ex) {
                System.out.println(ex.getMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            if (accountDTO == null || accountDTO.email() == null || accountDTO.email().isBlank()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unprocessable email");
                return;
            }

            String email = accountDTO.email().trim().toLowerCase();

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email, accountDTO.password());
            try {
                authenticationManager.authenticate(authenticationToken);
            } catch (AuthenticationException ex) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Failed");
                return;
            }

            long expirationTime = Instant.now().getEpochSecond() + jwtValidityPeriod;
            Map<String, String> claims = new HashMap<>();
            claims.put("sub", email);
            claims.put("iat", String.valueOf(Instant.now().getEpochSecond()));
            claims.put("exp", String.valueOf(expirationTime));
            String token;

            try {
                token = jwtService.generateJwt(claims);
            } catch (JwtErrorException ex) {
                System.out.println(ex.getMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Please try again.");
                return;
            }

            Account account = accountRepository.findByEmailAndIsEnabledAndIsNonLocked(email, true, true);
            if (account == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not found or not enabled or locked user");
                return;
            }

            response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean matchRequest(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod()) || SecurityContextHolder.getContext().getAuthentication() != null)
            return false;

        return "/api/signin".equals(request.getServletPath()) || "/api/signin/".equals(request.getServletPath());
    }

}
