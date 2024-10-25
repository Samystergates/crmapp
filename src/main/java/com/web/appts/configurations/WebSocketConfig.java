
package com.web.appts.configurations;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    String ipString = "http://172.22.250.26:3000";
//    String ipString = "http://192.168.100.146:3000";

    public WebSocketConfig() {
    }

    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(new String[]{"/topic"});
        config.setApplicationDestinationPrefixes(new String[]{"/app"});
    }

    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(new String[]{"/ws"}).setAllowedOriginPatterns(new String[]{ipString}).withSockJS();
    }
}
