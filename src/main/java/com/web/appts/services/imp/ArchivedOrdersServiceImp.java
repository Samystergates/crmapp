
package com.web.appts.services.imp;

import com.web.appts.DTO.ArchivedOrdersDto;
import com.web.appts.DTO.OrderDto;
import com.web.appts.entities.ArchivedOrders;
import com.web.appts.exceptions.ResourceNotFoundException;
import com.web.appts.repositories.ArchivedOrderRepo;
import com.web.appts.services.ArchivedOrdersService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

@Service
public class ArchivedOrdersServiceImp implements ArchivedOrdersService {
    Map<String, ArchivedOrdersDto> archivedOrdersMap = new HashMap();
    List<ArchivedOrdersDto> archivedOrderDtoList;
    String lastOrder;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ArchivedOrderRepo archivedOrdersRepo;
    private static final Logger logger = LoggerFactory.getLogger(ArchivedOrdersServiceImp.class);


    public ArchivedOrdersServiceImp() {
    }

    @Transactional
    public Boolean createArchivedOrder(OrderDto orderDto) {
        if (!this.archivedOrdersMap.isEmpty() && this.archivedOrdersMap.containsKey(orderDto.getOrderNumber() + "," + orderDto.getRegel())) {
            System.out.println("returning true1");
            return true;
        }
        ArchivedOrders archivedOrder = this.orderDtoToArchivedOrder(orderDto);
        ArchivedOrders savedArchivedOrder = (ArchivedOrders) this.archivedOrdersRepo.save(archivedOrder);
        ArchivedOrdersDto archivedOrdersDto = this.archivedOrderToDto(archivedOrder);
        this.archivedOrdersMap.put(orderDto.getOrderNumber() + "," + orderDto.getRegel(), archivedOrdersDto);
        System.out.println("returning true2" + savedArchivedOrder != null);
        return savedArchivedOrder != null;
    }

    public ArchivedOrdersDto getArchivedOrderById(Long orderId) {
        ArchivedOrders order = (ArchivedOrders) this.archivedOrdersRepo.findById(orderId).orElseThrow(() -> {
            return new ResourceNotFoundException("User", "id", (long) orderId.intValue());
        });
        return this.archivedOrderToDto(order);
    }

    public List<ArchivedOrdersDto> getArchivedOrdersByUser(String user) {
        if (this.archivedOrdersMap.isEmpty()) {
            List<ArchivedOrders> archivedOrdersByUser = this.archivedOrdersRepo.findByUser(user);
            List<ArchivedOrdersDto> archivedOrderDtos = (List) archivedOrdersByUser.stream().map((order) -> {
                return this.archivedOrderToDto(order);
            }).collect(Collectors.toList());
            return (List) (!archivedOrderDtos.isEmpty() && archivedOrderDtos != null ? archivedOrderDtos : new ArrayList());
        } else {
            if (this.archivedOrderDtoList == null || this.archivedOrderDtoList.size() != (new ArrayList(this.archivedOrdersMap.values())).size()) {
                this.archivedOrderDtoList = new ArrayList();
                Iterator var2 = this.archivedOrdersMap.entrySet().iterator();

                while (var2.hasNext()) {
                    Map.Entry<String, ArchivedOrdersDto> entry = (Map.Entry) var2.next();
                    if (((ArchivedOrdersDto) entry.getValue()).getUser().equals(user)) {
                        ArchivedOrdersDto archivedOrdersDto = (ArchivedOrdersDto) entry.getValue();
                        this.archivedOrderDtoList.add(archivedOrdersDto);
                    }
                }
            }

            return this.archivedOrderDtoList;
        }
    }

    //@Transactional
    public void deleteFromArchive(String orderNumber, String regel) {

        LocalDateTime currentDateTime = LocalDateTime.now();
        logger.info("Current DateTime: " + currentDateTime);
        logger.info("deleting from archive: " + orderNumber + ", " + regel);

        this.getAllArchivedOrders().forEach(ao -> {
            if (ao.getOrderNumber().equals(orderNumber) && ao.getRegel().equals((regel))) {
                try {
                    archivedOrdersRepo.deleteById(ao.getId());
                }catch (Exception e){
                    logger.info("exc from delete archive: "+e.getMessage());
                }
                //archivedOrdersRepo.deleteById(ao.getId());
            }
        });
        archivedOrdersMap.entrySet().removeIf(entry -> entry.getValue().getOrderNumber().equals(orderNumber) && entry.getValue().getRegel().equals(regel));
        archivedOrderDtoList.removeIf(order -> order.getOrderNumber().equals(orderNumber) && order.getRegel().equals((regel)));
    }

    @PostConstruct
    @Transactional
    public void init() {
        // Your method to be called on startup
        System.out.println("App started and init method called");
        getAllArchivedOrders();
    }

    //@Transactional
    public List<ArchivedOrdersDto> getAllArchivedOrders() {
        if (this.archivedOrdersMap.isEmpty()) {
            List<ArchivedOrders> allArchivedOrders = this.archivedOrdersRepo.findAll();
            if (allArchivedOrders == null || allArchivedOrders.isEmpty()) {
                return new ArrayList();
            }

            List<ArchivedOrdersDto> archivedOrderDtos = (List) allArchivedOrders.stream().map((order) -> {
                return this.archivedOrderToDto(order);
            }).collect(Collectors.toList());
            Iterator var3 = archivedOrderDtos.iterator();

            while (var3.hasNext()) {
                ArchivedOrdersDto archivedOrderDto = (ArchivedOrdersDto) var3.next();
                this.archivedOrdersMap.put(archivedOrderDto.getOrderNumber() + "," + archivedOrderDto.getRegel(), archivedOrderDto);
            }
        }

        if (this.archivedOrderDtoList == null || this.archivedOrderDtoList.size() != (new ArrayList(this.archivedOrdersMap.values())).size()) {
            this.archivedOrderDtoList = new ArrayList();
            Iterator var5 = this.archivedOrdersMap.entrySet().iterator();

            while (var5.hasNext()) {
                Map.Entry<String, ArchivedOrdersDto> entry = (Map.Entry) var5.next();
                ArchivedOrdersDto archivedOrderDto = (ArchivedOrdersDto) entry.getValue();
                this.archivedOrderDtoList.add(archivedOrderDto);
            }
        }

        return this.archivedOrderDtoList;
    }

    public void validateArchiveMap() {
        List<ArchivedOrdersDto> mapOrders = new ArrayList(this.archivedOrdersMap.values());
        long dataLength = this.archivedOrdersRepo.count();
        if (dataLength != (long) mapOrders.size()) {
            List<ArchivedOrders> allArchivedOrders = this.archivedOrdersRepo.findAll();
            if (allArchivedOrders.isEmpty() || allArchivedOrders == null) {
                System.out.println("error on validating archive order");
            }

            List<ArchivedOrdersDto> archivedOrderDtos = (List) allArchivedOrders.stream().map((order) -> {
                return this.archivedOrderToDto(order);
            }).collect(Collectors.toList());
            Iterator var6 = archivedOrderDtos.iterator();

            while (var6.hasNext()) {
                ArchivedOrdersDto archivedOrderDto = (ArchivedOrdersDto) var6.next();
                this.archivedOrdersMap.put(archivedOrderDto.getOrderNumber() + "," + archivedOrderDto.getRegel(), archivedOrderDto);
            }
        }

    }

    public ArchivedOrders dtoToArchivedOrder(ArchivedOrdersDto archivedOrderDto) {
        ArchivedOrders archivedOrder = (ArchivedOrders) this.modelMapper.map(archivedOrderDto, ArchivedOrders.class);
        return archivedOrder;
    }

    public ArchivedOrders orderDtoToArchivedOrder(OrderDto orderDto) {
        ArchivedOrders archivedOrder = (ArchivedOrders) this.modelMapper.map(orderDto, ArchivedOrders.class);
        return archivedOrder;
    }

    public ArchivedOrdersDto archivedOrderToDto(ArchivedOrders archivedOrder) {
        ArchivedOrdersDto archivedOrderDto = (ArchivedOrdersDto) this.modelMapper.map(archivedOrder, ArchivedOrdersDto.class);
        return archivedOrderDto;
    }
}
