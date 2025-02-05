
package com.web.appts.services.imp;

import com.web.appts.DTO.WheelColorDto;
import com.web.appts.DTO.WheelMachineSizeDto;
import com.web.appts.entities.WheelColor;
import com.web.appts.entities.WheelMachineSize;
import com.web.appts.exceptions.ResourceNotFoundException;
import com.web.appts.repositories.WheelColorRepo;
import com.web.appts.repositories.WheelMachineSizeRepo;
import com.web.appts.services.WheelColorService;
import com.web.appts.services.WheelMachineSizeService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WheelServices implements WheelMachineSizeService, WheelColorService {
	@Autowired
	WheelColorRepo wheelColorRepo;
	@Autowired
	WheelMachineSizeRepo wheelMachineSizeRepo;
	@Autowired
	private ModelMapper modelMapper;
	List<WheelMachineSizeDto> machineList = new ArrayList();
	List<WheelColorDto> colorList = new ArrayList();

	public WheelServices() {
	}

	public WheelMachineSizeDto createWheelMachineSize(WheelMachineSizeDto wheelMachineSizeDto) {
		WheelMachineSize wheelMachineSize = this.dtoToMachine(wheelMachineSizeDto);
		WheelMachineSize savedWheelMachineSize = (WheelMachineSize)this.wheelMachineSizeRepo.save(wheelMachineSize);
		if (!this.machineList.isEmpty()) {
			boolean idExists = this.machineList.stream().anyMatch((size) -> {
				return size.getId() == savedWheelMachineSize.getId();
			});
			if (!idExists) {
				this.machineList.add(this.machineToDto(savedWheelMachineSize));
			}
		}

		return this.machineToDto(savedWheelMachineSize);
	}

	public WheelMachineSizeDto updateWheelMachineSize(WheelMachineSizeDto wheelMachineSizeDto) {
		WheelMachineSize wheelMachineSize = this.dtoToMachine(wheelMachineSizeDto);
		WheelMachineSize savedWheelMachineSize = (WheelMachineSize)this.wheelMachineSizeRepo.save(wheelMachineSize);
		List<WheelMachineSizeDto> filteredList = (List)this.machineList.stream().filter((size) -> {
			return size.getId() != wheelMachineSizeDto.getId();
		}).collect(Collectors.toList());
		this.machineList.clear();
		this.machineList.addAll(filteredList);
		this.machineList.add(this.machineToDto(savedWheelMachineSize));
		return this.machineToDto(savedWheelMachineSize);
	}

	public Boolean deleteWheelMachineSize(Long wheelMachineSizeId) {
		WheelMachineSize wheelMachineSize = (WheelMachineSize)this.wheelMachineSizeRepo.findById(wheelMachineSizeId).orElseThrow(() -> {
			return new ResourceNotFoundException("wheelMachineSize", "id", (long)wheelMachineSizeId.intValue());
		});
		this.wheelMachineSizeRepo.delete(wheelMachineSize);
		List<WheelMachineSizeDto> filteredList = (List)this.machineList.stream().filter((size) -> {
			return size.getId() != wheelMachineSizeId;
		}).collect(Collectors.toList());
		this.machineList.clear();
		this.machineList.addAll(filteredList);
		return true;
	}

	public List<WheelMachineSizeDto> getAllWheelMachineSize() {
		if (this.machineList.isEmpty()) {
			List<WheelMachineSize> allMachines = this.wheelMachineSizeRepo.findAll();
			if (allMachines.isEmpty() || allMachines == null) {
				return null;
			}

			this.machineList = (List)allMachines.stream().map((machine) -> {
				return this.machineToDto(machine);
			}).collect(Collectors.toList());
		}

		return this.machineList;
	}

	public WheelColorDto createWheelColor(WheelColorDto wheelColorDto) {
		WheelColor wheelColor = this.dtoToColor(wheelColorDto);
		WheelColor savedWheelColor = (WheelColor)this.wheelColorRepo.save(wheelColor);
		if (!this.colorList.isEmpty()) {
			boolean idExists = this.colorList.stream().anyMatch((color) -> {
				return color.getId() == wheelColorDto.getId();
			});
			if (!idExists) {
				this.colorList.add(this.colorToDto(savedWheelColor));
			}
		}

		return this.colorToDto(savedWheelColor);
	}

	public WheelColorDto updateWheelColor(WheelColorDto wheelColorDto) {
		WheelColor wheelColor = this.dtoToColor(wheelColorDto);
		WheelColor savedWheelColor = (WheelColor)this.wheelColorRepo.save(wheelColor);
		List<WheelColorDto> filteredList = (List)this.colorList.stream().filter((color) -> {
			return !color.getId().equals(wheelColorDto.getId());
		}).collect(Collectors.toList());
		this.colorList.clear();
		this.colorList.addAll(filteredList);
		this.colorList.add(this.colorToDto(savedWheelColor));
		return this.colorToDto(savedWheelColor);
	}

	public Boolean deleteWheelColor(String wheelColorId) {
		WheelColor wheelColor = (WheelColor)this.wheelColorRepo.findById(wheelColorId).orElseThrow(() -> {
			return new ResourceNotFoundException("WheelColor", "id", wheelColorId);
		});
		this.wheelColorRepo.delete(wheelColor);
		List<WheelColorDto> filteredList = (List)this.colorList.stream().filter((color) -> {
			return !color.getId().equals(wheelColorId);
		}).collect(Collectors.toList());
		this.colorList.clear();
		this.colorList.addAll(filteredList);
		return true;
	}

	public List<WheelColorDto> getAllWheelColors() {
		if (this.colorList.isEmpty()) {
			List<WheelColor> allColors = this.wheelColorRepo.findAll();
			if (allColors.isEmpty() || allColors == null) {
				return null;
			}

			this.colorList = (List)allColors.stream().map((color) -> {
				return this.colorToDto(color);
			}).collect(Collectors.toList());
		}

		return this.colorList;
	}

	public WheelMachineSize dtoToMachine(WheelMachineSizeDto wheelMachineSizeDto) {
		WheelMachineSize wheelMachineSize = (WheelMachineSize)this.modelMapper.map(wheelMachineSizeDto, WheelMachineSize.class);
		return wheelMachineSize;
	}

	public WheelMachineSizeDto machineToDto(WheelMachineSize wheelMachineSize) {
		WheelMachineSizeDto wheelMachineSizeDto = (WheelMachineSizeDto)this.modelMapper.map(wheelMachineSize, WheelMachineSizeDto.class);
		return wheelMachineSizeDto;
	}

	public WheelColor dtoToColor(WheelColorDto wheelColorDto) {
		WheelColor WheelColor = (WheelColor)this.modelMapper.map(wheelColorDto, WheelColor.class);
		return WheelColor;
	}

	public WheelColorDto colorToDto(WheelColor wheelColor) {
		WheelColorDto wheelColorDto = (WheelColorDto)this.modelMapper.map(wheelColor, WheelColorDto.class);
		return wheelColorDto;
	}
}
