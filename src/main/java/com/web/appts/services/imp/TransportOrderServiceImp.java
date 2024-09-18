
package com.web.appts.services.imp;

import com.web.appts.DTO.DriverInfoDto;
import com.web.appts.DTO.RouteInfoDto;
import com.web.appts.DTO.TrailerInfoDto;
import com.web.appts.DTO.TruckInfoDto;
import com.web.appts.entities.DriverInfo;
import com.web.appts.entities.RouteInfo;
import com.web.appts.entities.TrailerInfo;
import com.web.appts.entities.TruckInfo;
import com.web.appts.repositories.DriverRepo;
import com.web.appts.repositories.RouteRepo;
import com.web.appts.repositories.TrailerRepo;
import com.web.appts.repositories.TruckRepo;
import com.web.appts.services.TransportOrderService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransportOrderServiceImp implements TransportOrderService {
	@Autowired
	DriverRepo driverRepo;
	@Autowired
	TrailerRepo trailerRepo;
	@Autowired
	TruckRepo truckRepo;
	@Autowired
	RouteRepo routeRepo;
	List<RouteInfoDto> routeList = new ArrayList();
	List<DriverInfoDto> driverList = new ArrayList();
	List<TruckInfoDto> truckList = new ArrayList();
	List<TrailerInfoDto> trailerList = new ArrayList();
	@Autowired
	private ModelMapper modelMapper;

	public TransportOrderServiceImp() {
	}

	public List<RouteInfoDto> getAllRoutes() {
		if (this.routeList.isEmpty()) {
			List<RouteInfo> allRoutes = this.routeRepo.findAll();
			if (allRoutes.isEmpty() || allRoutes == null) {
				return null;
			}

			this.routeList = (List)allRoutes.stream().map((route) -> {
				return this.routeToDto(route);
			}).collect(Collectors.toList());
		}

		return this.routeList;
	}

	public List<DriverInfoDto> getAllDrivers() {
		if (this.driverList.isEmpty()) {
			List<DriverInfo> allDrivers = this.driverRepo.findAll();
			if (allDrivers.isEmpty() || allDrivers == null) {
				return null;
			}

			this.driverList = (List)allDrivers.stream().map((driver) -> {
				return this.driverToDto(driver);
			}).collect(Collectors.toList());
		}

		return this.driverList;
	}

	public List<TrailerInfoDto> getAllTrailers() {
		if (this.trailerList.isEmpty()) {
			List<TrailerInfo> allTrailers = this.trailerRepo.findAll();
			if (allTrailers.isEmpty() || allTrailers == null) {
				return null;
			}

			this.trailerList = (List)allTrailers.stream().map((trailer) -> {
				return this.trailerToDto(trailer);
			}).collect(Collectors.toList());
		}

		return this.trailerList;
	}

	public List<TruckInfoDto> getAllTrucks() {
		if (this.truckList.isEmpty()) {
			List<TruckInfo> allTrucks = this.truckRepo.findAll();
			if (allTrucks.isEmpty() || allTrucks == null) {
				return null;
			}

			this.truckList = (List)allTrucks.stream().map((truck) -> {
				return this.truckToDto(truck);
			}).collect(Collectors.toList());
		}

		return this.truckList;
	}

	public RouteInfo dtoToRoute(RouteInfoDto routeInfoDto) {
		RouteInfo routeInfo = (RouteInfo)this.modelMapper.map(routeInfoDto, RouteInfo.class);
		return routeInfo;
	}

	public RouteInfoDto routeToDto(RouteInfo routeInfo) {
		RouteInfoDto routeInfoDto = (RouteInfoDto)this.modelMapper.map(routeInfo, RouteInfoDto.class);
		return routeInfoDto;
	}

	public DriverInfo dtoToDriver(DriverInfoDto driverInfoDto) {
		DriverInfo driverInfo = (DriverInfo)this.modelMapper.map(driverInfoDto, DriverInfo.class);
		return driverInfo;
	}

	public DriverInfoDto driverToDto(DriverInfo driverInfo) {
		DriverInfoDto driverInfoDto = (DriverInfoDto)this.modelMapper.map(driverInfo, DriverInfoDto.class);
		return driverInfoDto;
	}

	public TruckInfo dtoToTruck(TruckInfoDto truckInfoDto) {
		TruckInfo truckInfo = (TruckInfo)this.modelMapper.map(truckInfoDto, TruckInfo.class);
		return truckInfo;
	}

	public TruckInfoDto truckToDto(TruckInfo truckInfo) {
		TruckInfoDto truckInfoDto = (TruckInfoDto)this.modelMapper.map(truckInfo, TruckInfoDto.class);
		return truckInfoDto;
	}

	public TrailerInfo dtoToTrailer(TrailerInfoDto trailerInfoDto) {
		TrailerInfo trailerInfo = (TrailerInfo)this.modelMapper.map(trailerInfoDto, TrailerInfo.class);
		return trailerInfo;
	}

	public TrailerInfoDto trailerToDto(TrailerInfo trailerInfo) {
		TrailerInfoDto trailerInfoDto = (TrailerInfoDto)this.modelMapper.map(trailerInfo, TrailerInfoDto.class);
		return trailerInfoDto;
	}
}
