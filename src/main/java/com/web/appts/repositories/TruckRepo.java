package com.web.appts.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.web.appts.entities.TruckInfo;

public interface TruckRepo extends JpaRepository<TruckInfo, Integer>{

}
