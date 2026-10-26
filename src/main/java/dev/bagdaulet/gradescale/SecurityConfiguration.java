package dev.bagdaulet.gradescale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfiguration {
  @Bean
  org.springframework.security.core.userdetails.UserDetailsService users() {
    return new org.springframework.security.provisioning.InMemoryUserDetailsManager();
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
        .csrf(c -> c.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
        .headers(
            h ->
                h.contentSecurityPolicy(
                        c ->
                            c.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'; object-src 'none'"))
                    .referrerPolicy(
                        r ->
                            r.policy(
                                org.springframework.security.web.header.writers
                                    .ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                    .permissionsPolicyHeader(
                        p -> p.policy("camera=(), microphone=(), geolocation=()")))
        .exceptionHandling(
            e ->
                e.accessDeniedHandler(
                    (request, response, denied) -> {
                      response.setStatus(403);
                      response.setContentType("application/json");
                      response
                          .getWriter()
                          .write(
                              "{\"error\":{\"code\":\"CSRF_FAILED\",\"message\":\"Refresh this page and try again.\"}}");
                    }));
    return http.build();
  }
}
