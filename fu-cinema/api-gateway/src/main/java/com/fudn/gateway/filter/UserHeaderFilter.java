package com.fudn.gateway.filter;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * F10.4: Filter chuyen tiep trusted user headers tu JWT hop le xuong downstream services.
 * - Loai bo toan bo header client gui len (X-User-Id, X-User-Email, X-User-Role) de chong gia mao (spoofing).
 * - Neu request da xac thuc qua JWT, nap lai cac header tren tu claims cua JWT.
 * - Neu request la public/unauthenticated, khong nap them identity headers.
 */
@Component
public class UserHeaderFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        ServerRequest modified = ServerRequest.from(request)
                .headers(headers -> {
                    // 1. Luon xoa cac header ma client co the gui len de spoofing
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USER_EMAIL);
                    headers.remove(HEADER_USER_ROLE);

                    // 2. Neu duoc xac thuc bang JWT, lay du lieu uy tin tu claims va truyen xuong downstream
                    if (auth instanceof JwtAuthenticationToken jwtAuth) {
                        Jwt jwt = jwtAuth.getToken();
                        Object uid = jwt.getClaim("uid");
                        String email = jwt.getSubject();
                        String role = jwt.getClaimAsString("role");

                        if (uid != null) {
                            headers.set(HEADER_USER_ID, String.valueOf(uid));
                        }
                        if (email != null) {
                            headers.set(HEADER_USER_EMAIL, email);
                        }
                        if (role != null) {
                            headers.set(HEADER_USER_ROLE, role);
                        }
                    }
                })
                .build();

        return next.handle(modified);
    }
}