
package com.web.appts.configurations;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.jsonwebtoken.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtTokenHelper jwtTokenHelper;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public JwtAuthenticationFilter() {
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestToken = request.getHeader("Authorization");
        Enumeration<String> headerNames = request.getHeaderNames();

        String username = null;
        String token = null;
        if (requestToken != null && requestToken.startsWith("Bearer")) {
            token = requestToken.substring(7);

            try {
                username = this.jwtTokenHelper.getUsernameFromToken(token);
            } catch (IllegalArgumentException var10) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("Unable to get Jwt token");
                        messagingTemplate.convertAndSend("/topic/jwtExc", "logout");
                    }
                }, 1500);
            } catch (ExpiredJwtException var11) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("Jwt token has expired");
                        messagingTemplate.convertAndSend("/topic/jwtExc", "logout");
                    }
                }, 1500);
            } catch (MalformedJwtException var12) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("Jwt token has Malformed");
                        messagingTemplate.convertAndSend("/topic/jwtExc", "logout");
                    }
                }, 1500);
            } catch (SignatureException signatureException) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("Invalid jwt signature");
                        messagingTemplate.convertAndSend("/topic/jwtExc", "logout");
                    }
                }, 1500);
            }
        } else {
            System.out.println("Jwt token does not begin with Bearer");
//            throw new RuntimeException("Network Error");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            if (this.jwtTokenHelper.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userDetails, (Object) null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken.setDetails((new WebAuthenticationDetailsSource()).buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            } else {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("Invalid jwt signature");
                        System.out.println("Invalid jwt token");
                        messagingTemplate.convertAndSend("/topic/jwtExc", "logout");
                    }
                }, 1500);
            }
        } else {
            System.out.println("Username is null or context is not null");
        }

        filterChain.doFilter(request, response);
    }
}
