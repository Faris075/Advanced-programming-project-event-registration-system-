package com.evently.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Admin route guard – second layer of protection after Spring Security.
 *
 * Checks that the authenticated principal is an admin (isAdmin == true)
 * before allowing any request through to /admin/** controllers.
 * Sends HTTP 403 if the check fails.
 *
 * OWNER: Mohamed Morsy
 * REGISTERED IN: WebMvcConfig (Faris)
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            response.sendRedirect("/auth/login");
            return false;
        }

        // The principal is a Spring Security UserDetails object loaded by SecurityConfig's UserDetailsService.
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            // Check the ROLE_ADMIN granted authority that SecurityConfig assigns.
            boolean isAdmin = ud.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required.");
                return false;
            }
        }

        return true;
    }
}
