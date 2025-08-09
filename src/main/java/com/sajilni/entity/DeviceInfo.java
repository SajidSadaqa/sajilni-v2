package com.sajilni.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "device_info")
public class DeviceInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_device_user"))
    private User user;

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

    @Column(length = 64)
    private String clientTimestamp; // optional raw timestamp string from client

    // --- JPA requires a no-arg constructor
    public DeviceInfo() {}

    // --- Getters/Setters (no Lombok to avoid IDE annotation issues)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getOsName() { return osName; }
    public void setOsName(String osName) { this.osName = osName; }

    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public String getClientTimestamp() { return clientTimestamp; }
    public void setClientTimestamp(String clientTimestamp) { this.clientTimestamp = clientTimestamp; }
}
