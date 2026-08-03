package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.domain.Customer;
import com.KEYSTONE.ServiceManagement.domain.Role;
import com.KEYSTONE.ServiceManagement.domain.User;
import com.KEYSTONE.ServiceManagement.dto.request.LoginRequest;
import com.KEYSTONE.ServiceManagement.dto.request.RegisterUserRequest;
import com.KEYSTONE.ServiceManagement.dto.response.AuthResponse;
import com.KEYSTONE.ServiceManagement.dto.response.UserResponse;
import com.KEYSTONE.ServiceManagement.exception.DuplicateResourceException;
import com.KEYSTONE.ServiceManagement.exception.NotFoundException;
import com.KEYSTONE.ServiceManagement.repository.CustomerRepository;
import com.KEYSTONE.ServiceManagement.repository.UserRepository;
import com.KEYSTONE.ServiceManagement.security.JwtService;
import com.KEYSTONE.ServiceManagement.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new NotFoundException("User not found"));

        String token = jwtService.generateToken(new SecurityUser(user));
        return new AuthResponse(token, UserResponse.from(user));
    }

    @Transactional
    public AuthResponse register(RegisterUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("A user with this email already exists");
        }

        Customer customer = null;
        if (request.role() == Role.CUSTOMER) {
            if (request.customerId() == null) {
                throw new IllegalArgumentException("customerId is required for CUSTOMER role users");
            }
            customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + request.customerId()));
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .customer(customer)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        String token = jwtService.generateToken(new SecurityUser(user));
        return new AuthResponse(token, UserResponse.from(user));
    }
}
