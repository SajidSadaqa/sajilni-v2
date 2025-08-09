package com.sajilni.service;

import com.sajilni.dto.RegisterDto;
import com.sajilni.entity.DeviceInfo;
import com.sajilni.entity.User;
import com.sajilni.repository.DeviceInfoRepository;
import com.sajilni.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
public class UserService {
    private final UserRepository users;
    private final DeviceInfoRepository devices;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, DeviceInfoRepository devices, PasswordEncoder encoder) {
        this.users = users;
        this.devices = devices;
        this.encoder = encoder;
    }

    @Transactional
    public User createUser(RegisterDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (users.existsByEmail(dto.getEmail())) {
            throw new IllegalStateException("user.exists");
        }
        User u = new User();
                u.setFirstName(dto.getFirstName());
                u.setLastName(dto.getLastName());
                u.setEmail(dto.getEmail().toLowerCase());
                u.setPasswordHash(encoder.encode(dto.getPassword()));
                u.setEnabled(false);
        users.save(u);

        if (dto.getPlatform() != null || dto.getModel() != null || dto.getOsName() != null) {
                        DeviceInfo d = new DeviceInfo();
                        d.setUser(u);
                        d.setPlatform(dto.getPlatform());
                        d.setSerialNumber(dto.getSerialNumber());
                        d.setModel(dto.getModel());
                        d.setOsName(dto.getOsName());
                        d.setOsVersion(dto.getOsVersion());
                        d.setClientTimestamp(dto.getClientTimestamp());
            devices.save(d);
        }
        return u;
    }

    public void enableUser(String email) {
        User u = users.findByEmail(email.toLowerCase()).orElseThrow();
        if (!u.isEnabled()) {
            u.setEnabled(true);
            users.save(u);
        }
    }

    public User findByEmailOrThrow(String email) {
            return users.findByEmail(email.toLowerCase())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
          }
}
