package com.sajilni.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "device_info")
public class DeviceInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_device_user"))
    private UserEntity userEntity;

    @Column(length = 32)
    private String platform;        // Web, Android, iOS

    @Column(length = 128)
    private String serialNumber;    // device serial (mobile only)

    @Column(length = 128)
    private String model;           // device model

    @Column(length = 64)
    private String osName;          // OS name

    @Column(length = 64)
    private String osVersion;       // OS version

    @Column(name = "client_timestamp", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private String clientTimestamp; // optional raw timestamp string from client


}
