package sv.ufg.sigeric.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/login", "/registro", "/seguimiento", "/estado",
                        "/api/**", "/css/**", "/js/**", "/actuator/health/**", "/error").permitAll()
                .requestMatchers("/panel").hasRole("MUNICIPAL")
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/reportes", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(l -> l.logoutSuccessUrl("/?salir").permitAll())
            // La API de pruebas de alta disponibilidad se llama desde scripts (curl), sin formulario
            .csrf(c -> c.ignoringRequestMatchers("/api/**"));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
