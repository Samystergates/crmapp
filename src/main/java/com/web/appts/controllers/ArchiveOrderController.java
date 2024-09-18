
package com.web.appts.controllers;

import com.web.appts.DTO.ArchivedOrdersDto;
import com.web.appts.services.ArchivedOrdersService;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/archive"})
@CrossOrigin
public class ArchiveOrderController {
    @Autowired
    private ArchivedOrdersService archivedOrdersService;

    public ArchiveOrderController() {
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<ArchivedOrdersDto>> getArchivedOrders() {
        this.archivedOrdersService.validateArchiveMap();
        List<ArchivedOrdersDto> archivedOrderDto = this.archivedOrdersService.getAllArchivedOrders();
        if (archivedOrderDto != null) {
            this.sortUsingDate(archivedOrderDto);
        }

        return ResponseEntity.ok(archivedOrderDto);
    }

    @GetMapping({"/search/{userName}"})
    public ResponseEntity<List<ArchivedOrdersDto>> getOrdersByUser(@PathVariable("userName") String userName) {
        List<ArchivedOrdersDto> archivedOrderDto = this.archivedOrdersService.getArchivedOrdersByUser(userName);
        if (archivedOrderDto != null) {
            this.sortUsingDate(archivedOrderDto);
        }

        return ResponseEntity.ok(archivedOrderDto);
    }

    private void sortUsingDate(List<ArchivedOrdersDto> archivedOrderDto) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        archivedOrderDto.sort(Comparator.comparing((order) -> {
            String deliveryDate = order.getDeliveryDate();
            return deliveryDate.isEmpty() ? order.getCreationDate() : deliveryDate;
        }, (date1, date2) -> {
            try {
                Date parsedDate1 = dateFormat.parse(date1);
                Date parsedDate2 = dateFormat.parse(date2);
                return parsedDate2.compareTo(parsedDate1);
            } catch (ParseException var5) {
                ParseException e = var5;
                e.printStackTrace();
                return 0;
            }
        }));
    }
}
