package com.dnd.campaignmanager.user;

import com.dnd.campaignmanager.common.ConflictException;
import com.dnd.campaignmanager.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEV_USERNAME = "dev";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String username, String email, String rawPassword) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
        return userRepository.save(new User(username, email, passwordEncoder.encode(rawPassword)));
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User getByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    @Transactional
    public User getOrCreateDevUser() {
        return userRepository.findByUsernameIgnoreCase(DEV_USERNAME)
                .orElseGet(() -> userRepository.save(new User(
                        DEV_USERNAME, "dev@localhost", passwordEncoder.encode(UUID.randomUUID().toString()))));
    }
}
