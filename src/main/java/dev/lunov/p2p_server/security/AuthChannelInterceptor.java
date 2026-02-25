package dev.lunov.p2p_server.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private dev.lunov.p2p_server.repository.UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = null;
            
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null) {
                authHeader = accessor.getFirstNativeHeader("authorization");
            }
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            } else if (accessor.getPasscode() != null && !accessor.getPasscode().isEmpty()) {
                token = accessor.getPasscode();
            }

            if (token != null) {
                try {
                    String username = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);
                    
                    if (jwtUtil.isTokenValid(token, username)) {
                        if (userRepository != null) {
                            java.util.Optional<dev.lunov.p2p_server.model.User> optUser = userRepository.findByUsername(username);
                            if (optUser.isPresent() && optUser.get().isBanned()) {
                                System.err.println("❌ Blocking WebSocket connection for banned user: " + username);
                                throw new IllegalArgumentException("User is banned until " + optUser.get().getBannedUntil());
                            }
                        }

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                username, null, Collections.singletonList(new SimpleGrantedAuthority(role))
                        );
                        accessor.setUser(auth);
                        System.out.println("✅ WebSocket authenticated for user: " + username);
                        return message;
                    }
                } catch (Exception e) {
                    System.err.println("❌ Invalid WebSocket JWT token: " + e.getMessage());
                }
            }
            
            System.err.println("❌ No Valid JWT Token Found For STOMP Connection. Headers received: " + accessor.getMessageHeaders());
            throw new IllegalArgumentException("No Valid JWT Token Found For STOMP Connection");
        }
        
        return message;
    }
}
