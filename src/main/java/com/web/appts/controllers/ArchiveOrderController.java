
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
        if (archivedOrderDto != null && !archivedOrderDto.isEmpty()) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            archivedOrderDto.sort(Comparator.comparing((ArchivedOrdersDto order) -> {
                if (order == null) return null; // Handle null objects
                String deliveryDate = order.getDeliveryDate();
                String dateToUse = (deliveryDate != null && !deliveryDate.isEmpty()) ? deliveryDate : order.getCreationDate();
                return dateToUse; // Return the date string for comparison
            }, (date1, date2) -> {
                try {
                    Date parsedDate1 = (date1 != null) ? dateFormat.parse(date1) : null;
                    Date parsedDate2 = (date2 != null) ? dateFormat.parse(date2) : null;
                    return Comparator.nullsLast(Date::compareTo).compare(parsedDate2, parsedDate1);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0; // Treat parse errors as equal
                }
            }));
        }
    }
}