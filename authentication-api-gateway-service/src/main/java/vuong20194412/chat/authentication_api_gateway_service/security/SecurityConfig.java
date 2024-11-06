package vuong20194412.chat.authentication_api_gateway_service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

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
        /*  set strength == 1 -> error: bad strength
            with strength == 4 -> password form: $2a$04$1oUMLxhMZja.OxS.msvcLOiVsTbs8XXqY1mR1M/MealTRYEW5CgY6
                                                 $2a$04$TZZT4QEOgTjkETDQlIwyFOCJAlCguwjuO2Ubn3OivXxgBmIJ1tkkK
                                                 $2a$04$eJoTTsHE3t6gUi0IZbJ76..34mEghOuDNDnHaZ/YiVRysY2mMdlTe
                                                 $2a$04$hmC4KHub/B9EcWwsoYfbMO6eU51FfZ3P414Dd7u2A4fZYSvG/jQSy
                                                 $2a$04$.ZFElaNHdz/eTtT.jU9Ioea7YBJIHc6apg.ISeWJvrNQHFspV2HHi
                                                 $algorithm_version$strength$salt/hash
        */
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
       AuthenticationManager authenticationManager = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class)
                                                        .authenticationProvider(authenticationProvider())
                                                        .build();
       System.out.println("@Bean authenticationManager(" + httpSecurity + ") return " + authenticationManager);

       return authenticationManager;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        if (this.jwtAuthenticationFilter.getAuthenticationManager() == null) {
            AuthenticationManager authenticationManager = authenticationManager(httpSecurity); // equal @Bean authenticationManager
            System.out.println("@Bean securityFilterChain(" + httpSecurity + ") get " + authenticationManager);
            this.jwtAuthenticationFilter.setAuthenticationManager(authenticationManager);
        }

        httpSecurity
            .securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(new RegexRequestMatcher("/api/user/?", HttpMethod.POST.toString())).permitAll();
                auth.requestMatchers(new RegexRequestMatcher("/api/signup/?", HttpMethod.POST.toString())).permitAll();
                auth.requestMatchers(new RegexRequestMatcher("/api/signup/confirm/[a-zA-Z0-9-_]+\\.[a-zA-Z0-9-_]+\\.[a-zA-Z0-9-_]+", HttpMethod.GET.toString())).permitAll();
                auth.requestMatchers("/api/**").authenticated();
            })
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthorizationFilter, LogoutFilter.class)
            .logout(logout -> {
                logout.logoutRequestMatcher(new RegexRequestMatcher("/api/logout/?", HttpMethod.POST.toString()));
                logout.addLogoutHandler(new LogoutHandlerImpl(jwtAuthorizationFilter));
                logout.logoutSuccessHandler(new LogoutSuccessHandlerImpl());
            });

        return httpSecurity.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChainNotApi(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return httpSecurity.build();
    }

}
