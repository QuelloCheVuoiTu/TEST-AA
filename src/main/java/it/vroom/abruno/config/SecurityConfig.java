package it.vroom.abruno.config;

import it.vroom.abruno.classi.AppRoleMapping;
import it.vroom.abruno.repository.AppRoleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private static final String USER_HEADER = "Remote-User";
    private final AppRoleRepository appRoleRepository;

    public SecurityConfig(AppRoleRepository appRoleRepository) {
        this.appRoleRepository = appRoleRepository;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(new AutheliaAuthenticationProvider(new AutheliaUserDetailsService(appRoleRepository))));
    }

    @Bean
    public RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter(AuthenticationManager authenticationManager) {
        RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader(USER_HEADER);
        filter.setAuthenticationManager(authenticationManager);
        filter.setExceptionIfHeaderMissing(false);
        return filter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RequestHeaderAuthenticationFilter filter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .addFilter(filter)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/home", "/css/**", "/js/**", "/images/**", "/*.png", "/*.jpg", "/*.ico").permitAll()
                        .requestMatchers("/api/homepage/**", "/api/tracks/**", "/api/events/**", "/api/trackpages/**").permitAll()
                        .requestMatchers("/pista/{id:\\d+}", "/info", "/event/view/{id:\\d+}", "/pista/**").permitAll()

                        // REGOLE ADMIN E MANAGER
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole("ADMIN", "TRACK_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole("ADMIN", "TRACK_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // ==============================================================================
    // Classi Interne di Supporto con LOG DI DEBUG
    // ==============================================================================

    public static class AutheliaUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {
        private final AppRoleRepository appRoleRepository;

        public AutheliaUserDetailsService(AppRoleRepository appRoleRepository) {
            this.appRoleRepository = appRoleRepository;
        }

        @Override
        public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws AuthenticationException {
            String username = (String) token.getPrincipal();

            System.out.println("[AUTHELIA-DEBUG] Header Remote-User ricevuto: " + username);

            if (username == null || username.trim().isEmpty()) {
                System.err.println("[AUTHELIA-DEBUG] ERRORE: Nessun username trovato nell'header!");
                return null;
            }

            // Recupero ruoli da MongoDB (collezione appRoleMapping)
            List<AppRoleMapping> mappings = appRoleRepository.findByUsername(username);
            System.out.println("[AUTHELIA-DEBUG] Documenti trovati nel DB per " + username + ": " + mappings.size());

            List<GrantedAuthority> authorities = mappings.stream()
                    .map(mapping -> mapping.getAppRole().trim())
                    .filter(role -> !role.isEmpty())
                    .map(role -> {
                        String roleName = "ROLE_" + role.toUpperCase();
                        System.out.println("[AUTHELIA-DEBUG] Mapping ruolo: " + roleName);
                        return new SimpleGrantedAuthority(roleName);
                    })
                    .collect(Collectors.toList());

            // Ruolo base se il DB è vuoto per questo utente
            if (authorities.isEmpty()) {
                System.out.println("[AUTHELIA-DEBUG] Nessun ruolo in DB. Assegno ROLE_USER.");
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            return new User(username, "N/A", authorities);
        }
    }

    public static class AutheliaAuthenticationProvider implements AuthenticationProvider {
        private final AutheliaUserDetailsService userDetailsService;

        public AutheliaAuthenticationProvider(AutheliaUserDetailsService userDetailsService) {
            this.userDetailsService = userDetailsService;
        }

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            UserDetails userDetails = userDetailsService.loadUserDetails((PreAuthenticatedAuthenticationToken) authentication);
            if (userDetails == null) return null;

            return new PreAuthenticatedAuthenticationToken(
                    userDetails,
                    authentication.getCredentials(),
                    userDetails.getAuthorities()
            );
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication);
        }
    }
}