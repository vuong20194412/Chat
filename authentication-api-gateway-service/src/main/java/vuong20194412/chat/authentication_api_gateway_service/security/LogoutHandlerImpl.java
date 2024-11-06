package vuong20194412.chat.authentication_api_gateway_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import java.io.IOException;

class LogoutHandlerImpl extends SecurityContextLogoutHandler {

    private final JwtAuthorizationFilter jwtAuthorizationFilter;

    LogoutHandlerImpl(JwtAuthorizationFilter jwtAuthorizationFilter) {
        this.jwtAuthorizationFilter = jwtAuthorizationFilter;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        System.out.println("getLogoutHandler() authentication = " + authentication);
        System.out.println("getLogoutHandler() SecurityContextHolder.getContext().getAuthentication() = " + SecurityContextHolder.getContext().getAuthentication());
        if (authentication != null) {
            String token = jwtAuthorizationFilter.extractToken(request);
            if (token != null) {
                jwtAuthorizationFilter.getJwtService().saveBlackJsonWebTokenHash(token);
            }
            super.logout(request, response, authentication);
        }
    }
}

class LogoutSuccessHandlerImpl extends HttpStatusReturningLogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        if (authentication != null) {
            response.getWriter().write("Log out success");
            super.onLogoutSuccess(request, response, authentication);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
