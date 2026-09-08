package ru.practicum.transfer.config;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public String getCurrentLogin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String login = jwt.getClaimAsString("preferred_username");

            if (login == null) {
                throw new IllegalStateException("Токен не содержит preferred_username");
            }
            return login;
        }

        throw new IllegalStateException("Текущий запрос не аутентифицирован через JWT");
    }

}