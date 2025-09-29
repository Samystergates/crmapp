
package com.web.appts.services;

import com.web.appts.DTO.StickerLabelDto;
import java.util.List;

public interface StickerLabelService {
    StickerLabelDto createStickerLabel(StickerLabelDto stickerLabelDto);

    StickerLabelDto updateStickerLabel(StickerLabelDto stickerLabelDto);

    Boolean deleteStickerLabel(Long orderStickerId);

    List<StickerLabelDto> getAllStickerLabels();

    byte[] generateStickerPdf(String key, String orderNumber);

    StickerLabelDto getOrderSticker(String prodNumber);
}
