package com.sajilni.service;

import com.sajilni.entity.User;
import com.sajilni.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Collections;
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository users;

    public UserDetailsServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = users.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        boolean enabled = user.isEnabled();
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!enabled)
                .authorities(Collections.emptyList())
                .build();
    }
}
