package com.sajilni.repository;

import com.sajilni.entity.DeviceInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceInfoRepository extends JpaRepository<DeviceInfoEntity, Long> {}
