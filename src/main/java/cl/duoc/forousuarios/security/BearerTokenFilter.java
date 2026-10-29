package cl.duoc.forousuarios.security;

import cl.duoc.forousuarios.dto.UsuarioResponse;
import cl.duoc.forousuarios.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class BearerTokenFilter extends OncePerRequestFilter {
    private final JwtService tokens;
    private final AuthService auth;
    private final ApiSecurityErrorWriter errors;

    public BearerTokenFilter(JwtService tokens, AuthService auth, ApiSecurityErrorWriter errors) {
        this.tokens = tokens;
        this.auth = auth;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "POST".equals(request.getMethod())
                && (path.equals("/api/auth/registro") || path.equals("/api/auth/login"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            if (!header.regionMatches(true, 0, "Bearer ", 0, 7) || header.substring(7).isBlank()) {
                errors.escribir(response, HttpStatus.UNAUTHORIZED);
                return;
            }
            try {
                UsuarioResponse identidad = auth.identidad(tokens.validar(header.substring(7).strip()));
                var authentication = new UsernamePasswordAuthenticationToken(identidad, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + identidad.rol().name())));
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (JwtException | AuthenticationException ex) {
                SecurityContextHolder.clearContext();
                errors.escribir(response, HttpStatus.UNAUTHORIZED);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
