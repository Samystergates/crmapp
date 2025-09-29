
package com.web.appts.controllers;

import com.web.appts.DTO.StickerLabelDto;
import com.web.appts.services.AppPrintService;
import com.web.appts.services.StickerLabelService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping({"/api/sticker"})
@CrossOrigin
public class StickerLabelController {
    @Autowired
    private StickerLabelService stickerLabelService;

    public StickerLabelController() {
    }

    @PostMapping({"/label/save"})
    public ResponseEntity<StickerLabelDto> saveSticker(@RequestBody StickerLabelDto stickerLabelDto) {
        StickerLabelDto stickerLabelDtoReturn = this.stickerLabelService.createStickerLabel(stickerLabelDto);
        return new ResponseEntity(stickerLabelDtoReturn, HttpStatus.CREATED);
    }

    @PutMapping({"/label/update"})
    public ResponseEntity<StickerLabelDto> updateSticker(@RequestBody StickerLabelDto stickerLabelDto) {
        StickerLabelDto stickerLabelDtoReturn = this.stickerLabelService.updateStickerLabel(stickerLabelDto);
        return new ResponseEntity(stickerLabelDtoReturn, HttpStatus.CREATED);
    }

    @PostMapping({"/label/get"})
    public ResponseEntity<StickerLabelDto> getSticker(@RequestBody StickerLabelDto stickerLabelDto) {
        StickerLabelDto stickerLabelDtoReturn = this.stickerLabelService.getOrderSticker(stickerLabelDto.getProduct());
        if (stickerLabelDtoReturn == null) {
            stickerLabelDtoReturn = stickerLabelDto;
        }

        return ResponseEntity.ok(stickerLabelDtoReturn);
    }

    @DeleteMapping({"/label/delete/{stickerId}"})
    public ResponseEntity<Boolean> deleteSticker(@PathVariable("stickerId") Long stickerId) {
        Boolean isDeleted = this.stickerLabelService.deleteStickerLabel(stickerId);
        return new ResponseEntity(isDeleted, HttpStatus.OK);
    }

    @GetMapping({"/label/getAll"})
    public ResponseEntity<List<StickerLabelDto>> getAllSticker() {
        List<StickerLabelDto> stickerLabels = this.stickerLabelService.getAllStickerLabels();
        return ResponseEntity.ok(stickerLabels);
    }

    @GetMapping({"/label/printPdf/{key}/{orderNumber}"})
    public ResponseEntity<byte[]> generateMonPdf(@PathVariable("key") String key, @PathVariable("orderNumber") String orderNumber) {
        byte[] pdfBytes = this.stickerLabelService.generateStickerPdf(key, orderNumber);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "orderSticker.pdf");
        headers.setContentLength((long)pdfBytes.length);
        AppPrintService.PerformPrint("Zebra ZM600 (203 dpi) - ZPL", pdfBytes, true);
        return new ResponseEntity(pdfBytes, headers, 200);
    }
}
