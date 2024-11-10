
package com.web.appts.services;

import com.web.appts.DTO.WheelColorDto;
import java.util.List;

public interface WheelColorService {
	WheelColorDto createWheelColor(WheelColorDto WheelColorDto);

	WheelColorDto updateWheelColor(WheelColorDto WheelColorDto);

	Boolean deleteWheelColor(String wheelColorId);

	List<WheelColorDto> getAllWheelColors();
}
