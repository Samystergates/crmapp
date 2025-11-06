
package com.web.appts.controllers;

import com.web.appts.DTO.DriverInfoDto;
import com.web.appts.DTO.OrderDto;
import com.web.appts.DTO.OrderTRADto;
import com.web.appts.DTO.RouteInfoDto;
import com.web.appts.DTO.TrailerInfoDto;
import com.web.appts.DTO.TruckInfoDto;
import com.web.appts.services.AppPrintService;
import com.web.appts.services.OrderService;
import com.web.appts.services.OrderTRAService;
import com.web.appts.services.TransportOrderService;
import com.web.appts.services.imp.MonPrintingServiceImp;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
@RequestMapping({"/api/transport"})
@CrossOrigin
public class TransportOrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private TransportOrderService transportOrderService;
    @Autowired
    private OrderTRAService orderTRAService;
    @Autowired
    private MonPrintingServiceImp monPrintingServiceImp;

    public TransportOrderController() {
    }

    @GetMapping({"/routes"})
    public ResponseEntity<List<RouteInfoDto>> getRoutes() {
        List<RouteInfoDto> routeInfoDto = this.transportOrderService.getAllRoutes();
        return ResponseEntity.ok(routeInfoDto);
    }

    @GetMapping({"/drivers"})
    public ResponseEntity<List<DriverInfoDto>> getDrivers() {
        List<DriverInfoDto> driverInfoDto = this.transportOrderService.getAllDrivers();
        return ResponseEntity.ok(driverInfoDto);
    }

    @GetMapping({"/trucks"})
    public ResponseEntity<List<TruckInfoDto>> getTrucks() {
        List<TruckInfoDto> truckInfoDto = this.transportOrderService.getAllTrucks();
        return ResponseEntity.ok(truckInfoDto);
    }

    @GetMapping({"/trailers"})
    public ResponseEntity<List<TrailerInfoDto>> getTrailers() {
        List<TrailerInfoDto> trailerInfoDto = this.transportOrderService.getAllTrailers();
        return ResponseEntity.ok(trailerInfoDto);
    }

    @GetMapping({"/route/orders"})
    public ResponseEntity<Map<String, OrderTRADto>> getRouteOrders() {
        Map<String, OrderTRADto> orderTRAMap = this.orderTRAService.getAllTraOrders();
        return this.formatKeys(orderTRAMap) == null ? ResponseEntity.ok(null) : ResponseEntity.ok(this.formatKeys(orderTRAMap));
    }

    @PostMapping({"/route/orders/save"})
    public ResponseEntity<OrderTRADto> saveOrdersTRA(@RequestBody OrderTRADto orderTRADto) {
        OrderTRADto orderTRADtoReturn = this.orderTRAService.createOrderTRA(orderTRADto);
        List<OrderDto> updatedOrders = this.orderService.getAllOrders();
        this.sortUsingDate(updatedOrders);
        this.messagingTemplate.convertAndSend("/topic/orderUpdate", updatedOrders);
        return new ResponseEntity(orderTRADtoReturn, HttpStatus.CREATED);
    }

    @GetMapping({"/route/getAll"})
    public ResponseEntity<List<OrderTRADto>> getAllTra() {
        Map<String, OrderTRADto> orderTRAMap = this.orderTRAService.getAllTraOrders();
        List<OrderTRADto> orderTRAList = new ArrayList(orderTRAMap.values());
        return ResponseEntity.ok(orderTRAList);
    }

    @PutMapping({"/route/orders/update"})
    public ResponseEntity<OrderTRADto> updateOrdersTRA(@RequestBody OrderTRADto orderTRADto) {
        OrderTRADto orderTRADtoReturn = this.orderTRAService.updateOrderTRA(orderTRADto);
        List<OrderDto> updatedOrders = this.orderService.getAllOrders();
        this.sortUsingDate(updatedOrders);
        this.messagingTemplate.convertAndSend("/topic/orderUpdate", updatedOrders);
        return new ResponseEntity(orderTRADtoReturn, HttpStatus.CREATED);
    }

    @GetMapping({"/route/orders/{routeOrderId}"})
    public ResponseEntity<OrderTRADto> getRouteOrder(@PathVariable("routeOrderId") Long routeOrderId) {
        OrderTRADto orderTRADtoReturn = this.orderTRAService.getOrderTRA(routeOrderId);
        return ResponseEntity.ok(orderTRADtoReturn);
    }

    @DeleteMapping({"/route/orders/delete/{routeOrderId}"})
    public ResponseEntity<Boolean> deleteOrderTRA(@PathVariable("routeOrderId") Long routeOrderId) {
        Boolean isDeleted = this.orderTRAService.deleteOrderTRA(routeOrderId);
        return new ResponseEntity(isDeleted, HttpStatus.OK);
    }

    @PutMapping({"/route/orders/save/colors/{entryId}"})
    public ResponseEntity<Boolean> updateOrdersTRAColors(@RequestBody String orderTraIds, @PathVariable("entryId") Long entryId) {
        Boolean orderTRADtoReturn = this.orderTRAService.updateOrderTRAColors(orderTraIds, entryId);
        List<OrderDto> updatedOrders = this.orderService.getAllOrders();
        this.sortUsingDate(updatedOrders);
        this.messagingTemplate.convertAndSend("/topic/orderUpdate", updatedOrders);
        return new ResponseEntity(orderTRADtoReturn, HttpStatus.OK);
    }

    @GetMapping({"/route/TRA/printPdf/{routeOrderId}"})
    public ResponseEntity<byte[]> generateTraPdf(@PathVariable("routeOrderId") Long routeOrderId) {
        OrderTRADto orderTRADtoReturn = this.orderTRAService.getOrderTRA(routeOrderId);
        byte[] pdfBytes = this.orderTRAService.generateTRAPdf(orderTRADtoReturn);
        System.out.println("After PDF generation");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "orderTra.pdf");
        headers.setContentLength((long) pdfBytes.length);
        AppPrintService.PerformPrint("Toshiba Studio Expeditie", pdfBytes);
        return new ResponseEntity(pdfBytes, headers, 200);
    }

    @GetMapping({"/route/MON/printPdf/{key}"})
    public ResponseEntity<byte[]> generateMonPdf(@PathVariable("key") String key) {
        byte[] pdfBytes = this.monPrintingServiceImp.generateMONPdf(key);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "orderMon.pdf");
        headers.setContentLength((long) pdfBytes.length);
        AppPrintService.PerformPrint("TOSHIBA Studio LB/EM", pdfBytes);
        return new ResponseEntity(pdfBytes, headers, 200);
    }

    public Map<String, OrderTRADto> formatKeys(Map<String, OrderTRADto> orderTRAMap) {
        Map<String, OrderTRADto> formattedMap = new HashMap();
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");
        if (orderTRAMap != null && !orderTRAMap.isEmpty()) {
            Iterator var5 = orderTRAMap.entrySet().iterator();

            while (var5.hasNext()) {
                Map.Entry<String, OrderTRADto> entry = (Map.Entry) var5.next();
                String key = (String) entry.getKey();
                OrderTRADto order = (OrderTRADto) entry.getValue();
                String[] parts = key.split(",");
                String idPart = parts[0];
                String datePart = parts[1];
                String locationPart = parts[2];

                try {
                    Date date = inputFormat.parse(datePart);
                    String formattedDate = outputFormat.format(date);
                    String formattedEntry = idPart + "  -  " + formattedDate + "  -  " + locationPart;
                    formattedMap.put(formattedEntry, order);
                } catch (Exception var16) {
                    Exception e = var16;
                    e.printStackTrace();
                }
            }

            return formattedMap;
        } else {
            return null;
        }
    }

    private void sortUsingDate(List<OrderDto> orderDto) {
        DateFormat format1 = new SimpleDateFormat("MM/dd/yyyy");
        DateFormat format2 = new SimpleDateFormat("yyyy-MM-dd");

        orderDto.sort(Comparator.comparing((OrderDto order) -> {
            String deliveryDate = order.getDeliveryDate();
            return deliveryDate.isEmpty() ? order.getCreationDate() : deliveryDate;
        }, (dateStr1, dateStr2) -> {
            try {
                Date date1 = tryParseDate(dateStr1, format1, format2);
                Date date2 = tryParseDate(dateStr2, format1, format2);
                return date2.compareTo(date1); // descending
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        }));
    }

    private Date tryParseDate(String dateStr, DateFormat... formats) throws ParseException {
        for (DateFormat format : formats) {
            try {
                return format.parse(dateStr);
            } catch (ParseException ignored) {
            }
        }
        throw new ParseException("Unparseable date: \"" + dateStr + "\"", 0);
    }
}
