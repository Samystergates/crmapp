
package com.web.appts.controllers;

import com.web.appts.DTO.CustomOrderDto;
import com.web.appts.DTO.DeleteCustOrderDto;
import com.web.appts.DTO.OrderDto;
import com.web.appts.services.OrderService;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.web.appts.services.OrderTRAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/index", "/api/home"})
@CrossOrigin
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderTRAService orderTRAService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public OrderController() {
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<OrderDto>> getOrders() {
        List<OrderDto> orderDto = this.orderService.getAllOrders();
        this.sortUsingDate(orderDto);
        return ResponseEntity.ok(orderDto);
    }

    @GetMapping({"/refresh/orders"})
    public ResponseEntity<List<OrderDto>> getCrmOrders() {
        List<OrderDto> orderDto = this.orderService.checkMap();
        this.orderService.markExpired();
        this.orderService.checkOrderExistence();
        orderDto = this.orderService.getCRMOrders();
        this.orderService.createMonSub();
        this.orderService.updateProductNotes();
        this.orderService.adjustParentOrders();
        List<OrderDto> orderDtos = this.orderService.checkMap();

        this.sortUsingDate(orderDtos);
        this.messagingTemplate.convertAndSend("/topic/orderUpdate", orderDtos);
        return ResponseEntity.ok(orderDtos);
    }

    @GetMapping({"/search/{userName}"})
    public ResponseEntity<List<OrderDto>> getOrdersByUser(@PathVariable("userName") String userName) {
        List<OrderDto> orderDto = this.orderService.getOrdersByUser(userName);
        this.sortUsingDate(orderDto);
        return ResponseEntity.ok(orderDto);
    }

    @PutMapping({"/update/{flowUpdate}"})
    public ResponseEntity<List<OrderDto>> updatingOrder(@RequestBody OrderDto orderDto, @PathVariable("flowUpdate") Boolean flowUpdate) {
        List<OrderDto> updatedOrders = this.orderService.updateOrder(orderDto, orderDto.getId(), flowUpdate);
        this.sortUsingDate(updatedOrders);
        new Thread(() -> {
            try {
                Thread.sleep(200);
                messagingTemplate.convertAndSend("/topic/orderUpdate", updatedOrders);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        return ResponseEntity.ok(updatedOrders);
    }

    @PostMapping({"/create"})
    public ResponseEntity<List<OrderDto>> creatingOrder(@RequestBody OrderDto orderDto) {
        if(this.orderService.createOrder(orderDto)){
            List<OrderDto> orderDtos = this.orderService.getAllOrders();
            this.sortUsingDate(orderDtos);
            messagingTemplate.convertAndSend("/topic/orderUpdate", orderDtos);
            return ResponseEntity.ok(orderDtos);
        }
        else{
            List<OrderDto> orderDtos = this.orderService.getAllOrders();
            this.sortUsingDate(orderDtos);
            messagingTemplate.convertAndSend("/topic/orderUpdate", orderDtos);
            return ResponseEntity.ok(orderDtos);
        }
    }

    @PostMapping({"/create/custom"})
    public ResponseEntity<CustomOrderDto> creatingCustomOrder(@RequestBody CustomOrderDto customOrderDto) {
        CustomOrderDto co = this.orderService.createCustomOrder(customOrderDto);
        return ResponseEntity.ok(co);
    }

    @DeleteMapping({"/delete/custom"})
    public ResponseEntity<Boolean> deletingCustomOrder(@RequestBody CustomOrderDto customOrderDto) {
        DeleteCustOrderDto dto = this.orderService.deleteCustomOrder(customOrderDto);
        if(dto.getTransportOrderLines() != null) {
            Boolean result2 = this.orderTRAService.deleteLineFromTra(dto.getTransportOrderLines());
        }
        return ResponseEntity.ok(dto.getResult());
    }

    @GetMapping({"/custom"})
    public ResponseEntity<List<CustomOrderDto>> getCustomOrders() {
        List<CustomOrderDto> cOrders = this.orderService.getAllCustomOrders();
        return ResponseEntity.ok(cOrders);
    }

    @PutMapping({"/update/colors/{orderNumber}/{orderDep}/{flowVal}"})
    public ResponseEntity<List<OrderDto>> updatingOrderColors(@RequestBody String orderStatus, @PathVariable("orderNumber") String orderNumber, @PathVariable("orderDep") String orderDep, @PathVariable("flowVal") String flowVal) {
        List<OrderDto> updatedOrders = this.orderService.updateOrderColors(orderNumber, orderDep, orderStatus, flowVal);
        this.sortUsingDate(updatedOrders);
        this.messagingTemplate.convertAndSend("/topic/orderUpdate", updatedOrders);
        return ResponseEntity.ok(updatedOrders);
    }

//    private void sortUsingDate(List<OrderDto> orderDto) {
//        if (orderDto != null && !orderDto.isEmpty()) {
//            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//            orderDto.sort(Comparator.comparing((order) -> {
//                String deliveryDate = order.getDeliveryDate();
//                return deliveryDate.isEmpty() ? order.getCreationDate() : deliveryDate;
//            }, (date1, date2) -> {
//                try {
//                    Date parsedDate1 = dateFormat.parse(date1);
//                    Date parsedDate2 = dateFormat.parse(date2);
//                    return parsedDate2.compareTo(parsedDate1);
//                } catch (ParseException var5) {
//                    ParseException e = var5;
//                    e.printStackTrace();
//                    return 0;
//                }
//            }));
//        }
//    }

    private void sortUsingDate(List<OrderDto> orderDto) {
        if (orderDto != null && !orderDto.isEmpty()) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            orderDto.sort(Comparator.comparing((OrderDto order) -> {
                if (order == null) return null; // Handle null orders
                String deliveryDate = order.getDeliveryDate();
                String dateToParse = (deliveryDate != null && !deliveryDate.isEmpty()) ? deliveryDate : order.getCreationDate();
                return dateToParse; // Return the date string for parsing
            }, (date1, date2) -> {
                try {
                    Date parsedDate1 = (date1 != null) ? dateFormat.parse(date1) : null;
                    Date parsedDate2 = (date2 != null) ? dateFormat.parse(date2) : null;
                    return Comparator.nullsLast(Date::compareTo).compare(parsedDate2, parsedDate1);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0; // Treat parse failures as equal
                }
            }));
        }
    }


    @GetMapping("/export-orders")
    public ResponseEntity<byte[]> exportOrders() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Generate Excel file
            orderService.generateExcelFile(out);

            // Build response with file data
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.xlsx")
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
