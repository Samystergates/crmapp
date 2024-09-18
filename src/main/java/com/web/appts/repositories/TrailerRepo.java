package com.web.appts.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.web.appts.entities.TrailerInfo;

public interface TrailerRepo extends JpaRepository<TrailerInfo, Integer>{

}
