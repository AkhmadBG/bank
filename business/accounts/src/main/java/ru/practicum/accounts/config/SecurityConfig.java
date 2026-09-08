package ru.practicum.accounts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity security) throws Exception {
        return security
                .csrf(CsrfConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/internal/**").hasAuthority("SCOPE_internal")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(customizer -> customizer
                        .jwt(jwtCustomizer -> {
                            JwtAuthenticationConverter authConverter = new JwtAuthenticationConverter();

                            authConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
                                List<String> roles = jwt.getClaimAsStringList("roles");
                                if (roles == null) {
                                    return List.of();
                                }
                                return roles.stream()
                                        .map(SimpleGrantedAuthority::new)
                                        .collect(Collectors.toList());
                            });

                            jwtCustomizer.jwtAuthenticationConverter(authConverter);
                        }))
                .build();
    }

}