package com.sajilni.service;

import com.sajilni.domain.request.RegisterReq;
import com.sajilni.entity.DeviceInfoEntity;
import com.sajilni.entity.UserEntity;
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
    public UserEntity createUser(RegisterReq registerReq) {
        if (!registerReq.getPassword().equals(registerReq.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (users.existsByEmail(registerReq.getEmail())) {
            throw new IllegalStateException("user.exists");
        }

        UserEntity u = new UserEntity();
        u.setFirstName(registerReq.getFirstName());
        u.setLastName(registerReq.getLastName());
        u.setEmail(registerReq.getEmail().toLowerCase());
        u.setPasswordHash(encoder.encode(registerReq.getPassword()));
        u.setEnabled(false);

        // Save the user first
        u = users.save(u);

        // Save device info if provided
        if (registerReq.getPlatform() != null || registerReq.getModel() != null || registerReq.getOsName() != null) {
            DeviceInfoEntity d = new DeviceInfoEntity();
            d.setUserEntity(u);
            d.setPlatform(registerReq.getPlatform());
            d.setSerialNumber(registerReq.getSerialNumber());
            d.setModel(registerReq.getModel());
            d.setOsName(registerReq.getOsName());
            d.setOsVersion(registerReq.getOsVersion());
            d.setClientTimestamp(registerReq.getClientTimestamp());
            devices.save(d);
        }

        return u;
    }

    @Transactional
    public UserEntity enableUser(String email) {
        UserEntity u = users.findByEmail(email.toLowerCase()).orElseThrow();
        if (!u.isEnabled()) {
            u.setEnabled(true);
            u = users.save(u);
        }
        return u;
    }

    public UserEntity findByEmailOrThrow(String email) {
        return users.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}