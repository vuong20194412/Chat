package vuong20194412.chat.authentication_api_gateway_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import java.time.Instant;

@Configuration
class SecurityConfig {

    private final UserDetailsService userDetailsService;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final JwtAuthorizationFilter jwtAuthorizationFilter;

    @Autowired
    SecurityConfig(UserDetailsService userDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter, JwtAuthorizationFilter jwtAuthorizationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthorizationFilter = jwtAuthorizationFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        // set strength == 1 -> error: bad strength
        return new BCryptPasswordEncoder(4);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) throws Exception {
       return httpSecurity.getSharedObject(AuthenticationManagerBuilder.class)
               .authenticationProvider(authenticationProvider())
               .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        if (this.jwtAuthenticationFilter.getAuthenticationManager() == null)
            this.jwtAuthenticationFilter.setAuthenticationManager(authenticationManager(httpSecurity));

        httpSecurity
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/signup", "/api/signup/**", "/api/user",
                                "/api/user/**").permitAll()
                                .anyRequest().authenticated()
                        //.requestMatchers("/api/**").authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
//                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//                .addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class)
//                .logout(logout -> logout
//                        .logoutUrl("/api/logout")
//                        .addLogoutHandler(getLogoutHandler()))
//                .logout(logout -> logout
//                        .logoutUrl("/api/logout/")
//                        .addLogoutHandler(getLogoutHandler()))
        ;

        return httpSecurity.build();
    }

    private LogoutHandler getLogoutHandler() {
        return (request, response, authentication) -> {
            if (authentication != null) {
                SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
                logoutHandler.logout(request, response, authentication);
                String token = jwtAuthorizationFilter.extractToken(request);
                if (token != null) {
                    Long now = Instant.now().getEpochSecond();
                    JsonWebToken jsonWebToken = jwtAuthorizationFilter.getJwtRepository().findByTokenAndExpirationTimeGreaterThanEqual(token, now);
                    if (jsonWebToken != null) {
                        jsonWebToken.setExpirationTime(now);
                    }
                }
            }
        };
    }

}
