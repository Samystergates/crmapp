package com.web.appts.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.web.appts.entities.DriverInfo;

public interface DriverRepo extends JpaRepository<DriverInfo, Integer>{

}
