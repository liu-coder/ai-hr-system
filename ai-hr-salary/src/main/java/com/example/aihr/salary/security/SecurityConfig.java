package com.example.aihr.salary.security;

import com.example.aihr.common.security.AuthContextFilter;
import com.example.aihr.common.security.ServiceJwtProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(ServiceJwtProps.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ServiceJwtProps props, ObjectMapper om) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(reg -> reg
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
        );
        http.addFilterBefore(new AuthContextFilter(props, om), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

