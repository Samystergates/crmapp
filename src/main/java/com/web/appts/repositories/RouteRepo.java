package com.web.appts.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.web.appts.entities.RouteInfo;

public interface RouteRepo extends JpaRepository<RouteInfo, Integer>{

}
