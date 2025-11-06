package com.web.appts.controllers;


import com.web.appts.DTO.OrderDto;
import com.web.appts.DTO.OrderSERDto;
import com.web.appts.services.AppPrintService;
import com.web.appts.services.OrderSerMechanicsService;
import com.web.appts.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping({"/api/ser"})
@CrossOrigin
public class OrderServiceMechanicsController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private OrderSerMechanicsService orderSerMechanicsService;

    @Autowired
    private OrderService orderService;

    @PostMapping({"/save"})
    public ResponseEntity<OrderSERDto> saveOrderSME(@RequestBody OrderSERDto orderSERDto) {
        OrderSERDto orderSERDtoReturn = this.orderSerMechanicsService.createOrderSER(orderSERDto);
        List<OrderDto> updatedOrders = this.orderService.getAllOrders();
        this.sortUsingDate(updatedOrders);
        this.messagingTemplate.convertAndSend("/topic/serSave", updatedOrders);
        return new ResponseEntity(orderSERDtoReturn, HttpStatus.CREATED);
    }

    @PutMapping({"/update"})
    public ResponseEntity<OrderSERDto> updateOrderSME(@RequestBody OrderSERDto orderSERDto) {
        OrderSERDto orderSERDtoReturn = this.orderSerMechanicsService.updateOrderSER(orderSERDto);
        List<OrderDto> updatedOrders = this.orderService.getAllOrders();
        this.sortUsingDate(updatedOrders);
        this.messagingTemplate.convertAndSend("/topic/serUpdate", updatedOrders);
        return new ResponseEntity(orderSERDtoReturn, HttpStatus.CREATED);
    }

    @PostMapping({"/get"})
    public ResponseEntity<OrderSERDto> getOrderSER(@RequestBody OrderSERDto orderSERDto) {
        OrderSERDto orderSERDtoReturn = this.orderSerMechanicsService.getOrderSER(orderSERDto.getOrderNumber());
        if (orderSERDtoReturn == null) {
            orderSERDtoReturn = orderSERDto;
        }

        return ResponseEntity.ok(orderSERDtoReturn);
    }

    @DeleteMapping({"/delete/{serId}"})
    public ResponseEntity<Boolean> deleteOrderSER(@PathVariable("serId") Long serId) {
        Boolean isDeleted = this.orderSerMechanicsService.deleteOrderSER(serId);
        List<OrderDto> updatedOrders = this.orderService.getAllOrders();
        this.sortUsingDate(updatedOrders);
        this.messagingTemplate.convertAndSend("/topic/serDelete", updatedOrders);
        return new ResponseEntity(isDeleted, HttpStatus.OK);
    }

    @GetMapping({"/getAll"})
    public ResponseEntity<List<OrderSERDto>> getSEROrders() {
        List<OrderSERDto> serList = this.orderSerMechanicsService.getAllSer();
        return ResponseEntity.ok(serList);
    }

    @GetMapping({"/monteur/getAll"})
    public ResponseEntity<List<String>> getMonteurs() {
        List<String> monteurList = this.orderSerMechanicsService.getAllMonteurs();
        return ResponseEntity.ok(monteurList);
    }

    @GetMapping({"/printPdf/{key}"})
    public ResponseEntity<byte[]> generateSpuPdf(@PathVariable("key") String key) {
        byte[] pdfBytes = this.orderSerMechanicsService.generateSERPdf(key);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "orderSer.pdf");
        headers.setContentLength((long)pdfBytes.length);
        AppPrintService.PerformPrint("TOSHIBA Studio LB/EM", pdfBytes);
        return new ResponseEntity(pdfBytes, headers, 200);
    }

    private void sortUsingDate(List<OrderDto> orderDto) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        orderDto.sort(Comparator.comparing((order) -> {
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
