
package com.web.appts.services;

import com.web.appts.DTO.WheelMachineSizeDto;
import java.util.List;

public interface WheelMachineSizeService {
	WheelMachineSizeDto createWheelMachineSize(WheelMachineSizeDto wheelMachineSizeDto);

	List<WheelMachineSizeDto> getAllWheelMachineSize();

	WheelMachineSizeDto updateWheelMachineSize(WheelMachineSizeDto wheelMachineSizeDto);

	Boolean deleteWheelMachineSize(Long wheelMachineSizeId);
}
