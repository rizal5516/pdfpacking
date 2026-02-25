package id.go.bssn.blpid.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String PROTECTED_PREFIX = "/pdfpack/";

    @Value("${app.api-key}")
    private String validApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Hanya lindungi endpoint /pdfpack/
        if (!path.startsWith(PROTECTED_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(API_KEY_HEADER);

        // Validasi: header tidak ada atau kosong
        if (providedKey == null || providedKey.isBlank()) {
            log.warn("[SECURITY] Request ke {} ditolak: header X-API-Key tidak ada. IP={}",
                    path, getClientIp(request));
            sendUnauthorized(response, "Header X-API-Key wajib disertakan");
            return;
        }

        // Validasi: API Key tidak cocok (gunakan constant-time comparison untuk cegah timing attack)
        if (!constantTimeEquals(validApiKey, providedKey)) {
            log.warn("[SECURITY] Request ke {} ditolak: API Key tidak valid. IP={}",
                    path, getClientIp(request));
            sendUnauthorized(response, "API Key tidak valid");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Perbandingan string constant-time untuk mencegah timing attack.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

    /**
     * Ambil IP client dengan mempertimbangkan proxy/load balancer.
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}