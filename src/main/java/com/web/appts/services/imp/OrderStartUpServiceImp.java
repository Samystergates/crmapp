package com.web.appts.services.imp;

import com.web.appts.DTO.OrderDto;
import com.web.appts.DTO.OrderSMEDto;
import com.web.appts.DTO.OrderSPUDto;
import com.web.appts.repositories.OrderSPURepo;
import com.web.appts.services.OrderSMEService;
import com.web.appts.services.OrderSPUService;
import com.web.appts.services.OrderService;
import com.web.appts.services.OrderStartUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OrderStartUpServiceImp implements OrderStartUpService {


    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderSMEService orderSMEService;

    @Autowired
    private OrderSPUService orderSPUService;

    @Override
    public void verifyUpdateAddSmeSpuDots() {
        List<OrderSMEDto> listSme = orderSMEService.getAllSme();
        List<OrderSPUDto> listSpu = orderSPUService.getAllSpu();

        List<String> smeKeys = listSme.stream().map(sme -> sme.getOrderNumber() + "," + sme.getRegel())
                .collect(Collectors.toList());

        List<String> spuKeys = listSpu.stream().map(spu -> spu.getOrderNumber() + "," + spu.getRegel())
                .collect(Collectors.toList());

        List<OrderDto> allOrders = orderService.getAllOrders();
        for (OrderDto orderDto : allOrders){

            if(smeKeys.contains(orderDto.getOrderNumber() + "," + orderDto.getRegel()) &&
                    (orderDto.getSme() != null && orderDto.getSme().isEmpty())){
                orderDto.setSme("R");
                orderService.updateOrder(orderDto, orderDto.getId(), true);
            }

            if(spuKeys.contains(orderDto.getOrderNumber() + "," + orderDto.getRegel()) &&
                    (orderDto.getSpu() != null && orderDto.getSpu().isEmpty())){
                orderDto.setSpu("R");
                orderService.updateOrder(orderDto, orderDto.getId(), true);
            }

        }

        Map<String, List<OrderDto>> groupedByOrder = allOrders.stream()
                .collect(Collectors.groupingBy(OrderDto::getOrderNumber));

        for (Map.Entry<String, List<OrderDto>> entry : groupedByOrder.entrySet()) {
            List<OrderDto> group = entry.getValue();

            updateDepartmentConsistency(group, "monlb");
            updateDepartmentConsistency(group, "montr");
            updateDepartmentConsistency(group, "mwe");
            updateDepartmentConsistency(group, "ser");
            updateDepartmentConsistency(group, "exp");
        }

    }


    private void updateDepartmentConsistency(List<OrderDto> group, String dep) {
        List<String> values = group.stream()
                .map(order -> getDepValue(order, dep))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (values.isEmpty()) return;

        String finalValue = getHighestPriority(values);

        for (OrderDto order : group) {
            String currentValue = getDepValue(order, dep);
            if (finalValue != null && (currentValue == null || !currentValue.equals(finalValue))) {
                setDepValue(order, dep, finalValue);
                orderService.updateOrder(order, order.getId(), true);
            }
        }
    }

    private String getHighestPriority(List<String> values) {
        if (values.contains("G")) return "G";
        if (values.contains("Y")) return "Y";
        if (values.contains("R")) return "R";
        return null;
    }

    private String getDepValue(OrderDto order, String dep) {
        if ("monlb".equals(dep)) return order.getMonLb();
        if ("montr".equals(dep)) return order.getMonTr();
        if ("mwe".equals(dep)) return order.getMwe();
        if ("ser".equals(dep)) return order.getSer();
        if ("exp".equals(dep)) return order.getExp();
        return null;
    }

    private void setDepValue(OrderDto order, String dep, String value) {
        if ("monlb".equals(dep)) order.setMonLb(value);
        if ("montr".equals(dep)) order.setMonTr(value);
        if ("mwe".equals(dep)) order.setMwe(value);
        else if ("ser".equals(dep)) order.setSer(value);
        else if ("exp".equals(dep)) order.setExp(value);
    }

}
