package bulktrack;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        var paths = PathPatternRequestMatcher.withDefaults();
        return http.authorizeHttpRequests(auth -> auth.requestMatchers("/assets/login.html", "/assets/**", "/csrf").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        (req, res, ex) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/problem+json");
                            res.getWriter().write("{\"status\":401,\"detail\":\"Entre na sua conta.\"}");
                        }, request -> paths.matcher("/perfis/**").matches(request)
                                || paths.matcher("/api/**").matches(request)))
                .formLogin(login -> login.loginPage("/assets/login.html").loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/assets/login.html?logout").permitAll()).build();
    }
}
