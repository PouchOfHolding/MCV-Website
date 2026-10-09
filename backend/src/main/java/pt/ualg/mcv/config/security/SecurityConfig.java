package pt.ualg.mcv.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig
{

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final UrlBasedCorsConfigurationSource corsConfigurationSource;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http)
        {
                http.csrf(csrf -> csrf.disable()).anonymous(anonymous -> {
                }).cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/login",
                                                                "/api/auth/refresh",
                                                                "/api/users/register",
                                                                "/api/health", "/error")
                                                .permitAll()
                                                .requestMatchers("/api/docs", "/api/docs/**",
                                                                "/api/swagger-ui",
                                                                "/api/swagger-ui/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/members/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/gallery/photos/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/uploads/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/families/**")
                                                .permitAll().anyRequest().authenticated())
                                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                                                (req, res, authEx) -> res.sendError(
                                                                HttpServletResponse.SC_UNAUTHORIZED)))
                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))
                                .addFilterBefore(jwtAuthFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(HttpSecurity http,
                        DaoAuthenticationProvider authProvider)
        {
                return http.getSharedObject(AuthenticationManagerBuilder.class)
                                .authenticationProvider(authProvider).build();
        }

        @Bean
        public PasswordEncoder passwordEncoder()
        {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider(
                        UserDetailsService userDetailsService, PasswordEncoder passwordEncoder)
        {
                DaoAuthenticationProvider authProvider =
                                new DaoAuthenticationProvider(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder);
                return authProvider;
        }
}
