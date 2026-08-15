package com.chatapp.api.service;

import com.chatapp.api.dto.*;
import com.chatapp.api.model.User;
import com.chatapp.api.repository.UserRepository;
import com.chatapp.api.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse register(UserCreate payload) {
        if (userRepository.findByEmail(payload.email()).isPresent()
            || userRepository.findByUsername(payload.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or username already taken");
        }

        User user = new User();
        user.setUsername(payload.username());
        user.setEmail(payload.email());
        user.setHashedPassword(passwordEncoder.encode(payload.password()));
        user.setDisplayName(payload.displayName() != null ? payload.displayName() : payload.username());
        user = userRepository.save(user);

        String accessToken = jwtService.createAccessToken(user.getId());
        String refreshToken = jwtService.createRefreshToken(user.getId());
        return new TokenResponse(accessToken, refreshToken, UserResponse.from(user));
    }

    @Transactional
    public TokenResponse login(UserLogin payload) {
        User user = userRepository.findByEmail(payload.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(payload.password(), user.getHashedPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }

        String accessToken = jwtService.createAccessToken(user.getId());
        String refreshToken = jwtService.createRefreshToken(user.getId());
        return new TokenResponse(accessToken, refreshToken, UserResponse.from(user));
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest payload) {
        var claims = jwtService.decodeToken(payload.refreshToken());
        if (claims == null || !"refresh".equals(claims.get("type", String.class))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found or inactive"));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found or inactive");
        }

        String accessToken = jwtService.createAccessToken(user.getId());
        String newRefreshToken = jwtService.createRefreshToken(user.getId());
        return new TokenResponse(accessToken, newRefreshToken, UserResponse.from(user));
    }

    public UserResponse getProfile(User user) {
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(User user, UserProfileUpdate payload) {
        if (payload.displayName() != null) {
            user.setDisplayName(payload.displayName());
        }
        if (payload.avatarUrl() != null) {
            user.setAvatarUrl(payload.avatarUrl());
        }
        if (payload.bio() != null) {
            user.setBio(payload.bio());
        }
        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(User user, PasswordChange payload) {
        if (!passwordEncoder.matches(payload.currentPassword(), user.getHashedPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setHashedPassword(passwordEncoder.encode(payload.newPassword()));
        userRepository.save(user);
    }

    public List<UserSearchResult> searchUsers(String query, UUID excludeId) {
        return userRepository.searchByUsernameOrDisplayName(query, excludeId)
            .stream()
            .map(UserSearchResult::from)
            .toList();
    }

    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserResponse.from(user);
    }
}
