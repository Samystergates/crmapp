
package com.web.appts.controllers;

import com.web.appts.DTO.WheelColorDto;
import com.web.appts.DTO.WheelMachineSizeDto;
import com.web.appts.services.WheelColorService;
import com.web.appts.services.WheelMachineSizeService;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/wheels"})
@CrossOrigin
public class WheelsController {
	@Autowired
	private WheelColorService wheelColorService;
	@Autowired
	private WheelMachineSizeService wheelMachineSizeService;

	public WheelsController() {
	}

	@PostMapping({"/machine-size/save"})
	public ResponseEntity<WheelMachineSizeDto> saveAllWheelMachineSize(@RequestBody WheelMachineSizeDto wheelMachineSizeDto) {
		WheelMachineSizeDto wheelMachineSizeDtoReturn = this.wheelMachineSizeService.createWheelMachineSize(wheelMachineSizeDto);
		return new ResponseEntity(wheelMachineSizeDtoReturn, HttpStatus.CREATED);
	}

	@PutMapping({"/machine-size/update"})
	public ResponseEntity<WheelMachineSizeDto> updateAllWheelMachineSize(@RequestBody WheelMachineSizeDto wheelMachineSizeDto) {
		WheelMachineSizeDto wheelMachineSizeDtoReturn = this.wheelMachineSizeService.updateWheelMachineSize(wheelMachineSizeDto);
		return new ResponseEntity(wheelMachineSizeDtoReturn, HttpStatus.CREATED);
	}

	@GetMapping({"/machine-size/getAll"})
	public ResponseEntity<List<WheelMachineSizeDto>> getAllWheelMachineSize() {
		List<WheelMachineSizeDto> wheelMachineSizeDto = this.wheelMachineSizeService.getAllWheelMachineSize();
		Collections.sort(wheelMachineSizeDto, Comparator.comparing(WheelMachineSizeDto::getId));
		return ResponseEntity.ok(wheelMachineSizeDto);
	}

	@DeleteMapping({"/machine-size/delete/{wheelMachineSizeId}"})
	public ResponseEntity<Boolean> deleteAllWheelMachineSize(@PathVariable("wheelMachineSizeId") Long wheelMachineSizeId) {
		Boolean isDeleted = this.wheelMachineSizeService.deleteWheelMachineSize(wheelMachineSizeId);
		return new ResponseEntity(isDeleted, HttpStatus.OK);
	}

	@PostMapping({"/wheel-color/save"})
	public ResponseEntity<WheelColorDto> saveAllWheelColor(@RequestBody WheelColorDto wheelColorDto) {
		WheelColorDto wheelColorDtoReturn = this.wheelColorService.createWheelColor(wheelColorDto);
		return new ResponseEntity(wheelColorDtoReturn, HttpStatus.CREATED);
	}

	@PutMapping({"/wheel-color/update"})
	public ResponseEntity<WheelColorDto> updateAllWheelColor(@RequestBody WheelColorDto wheelColorDto) {
		WheelColorDto wheelColorDtoReturn = this.wheelColorService.updateWheelColor(wheelColorDto);
		return new ResponseEntity(wheelColorDtoReturn, HttpStatus.CREATED);
	}

	@DeleteMapping({"/wheel-color/delete/{wheelColorId}"})
	public ResponseEntity<Boolean> deleteAllWheelColor(@PathVariable("wheelColorId") Long wheelColorId) {
		Boolean isDeleted = this.wheelColorService.deleteWheelColor(wheelColorId);
		return new ResponseEntity(isDeleted, HttpStatus.OK);
	}

	@GetMapping({"/wheel-color/getAll"})
	public ResponseEntity<List<WheelColorDto>> getAllWheelColor() {
		List<WheelColorDto> wheelColorDto = this.wheelColorService.getAllWheelColors();
		Collections.sort(wheelColorDto, Comparator.comparing(WheelColorDto::getId));
		return ResponseEntity.ok(wheelColorDto);
	}
}
