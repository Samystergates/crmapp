
package com.web.appts.services.imp;

import com.web.appts.DTO.ArchivedOrdersDto;
import com.web.appts.DTO.OrderDto;
import com.web.appts.entities.Order;
import com.web.appts.entities.OrderDepartment;
import com.web.appts.exceptions.ResourceNotFoundException;
import com.web.appts.repositories.OrderRepo;
import com.web.appts.services.ArchivedOrdersService;
import com.web.appts.services.OrderService;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImp implements OrderService {
    Map<String, OrderDto> ordersMap = new HashMap();
    Map<String, OrderDto> archivedOrdersMap = new HashMap();
    List<OrderDto> orderDtoList;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private OrderRepo orderRepo;
    @Autowired
    private ArchivedOrdersService archivedOrdersService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public OrderServiceImp() {
    }

    public Map<String, OrderDto> getMap() {
        if (this.ordersMap.isEmpty() || this.ordersMap == null) {
            List<Order> allOrders = this.orderRepo.findAll();
            List<OrderDto> orderDtos = (List) allOrders.stream().map((order) -> {
                order.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                OrderDto sortedDeps = this.orderToDto(order);
                return sortedDeps;
            }).collect(Collectors.toList());
            Iterator var3 = orderDtos.iterator();

            while (var3.hasNext()) {
                OrderDto orderDto = (OrderDto) var3.next();
                this.ordersMap.put(orderDto.getOrderNumber() + "," + orderDto.getRegel(), orderDto);
            }
        }

        return this.ordersMap;
    }

    public Boolean createOrder(OrderDto orderDto) {
        Order order = this.dtoToOrder(orderDto);
        Order savedOrder = (Order) this.orderRepo.save(order);
        return savedOrder != null;
    }

    @Transactional
    public Boolean archiveOrder(OrderDto orderDto) {
        return this.archivedOrdersService.createArchivedOrder(orderDto);
    }

    @Transactional
    public void moveToArchive(List<Integer> ids) {
        System.out.print("removing: ");
        for(Integer i : ids){
            System.out.print(i + ",");
        }
        Boolean isMoved = false;
        Iterator<Map.Entry<String, OrderDto>> iterator = this.ordersMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, OrderDto> entry = (Map.Entry) iterator.next();
            OrderDto orderDto = (OrderDto) entry.getValue();
            if (ids.contains(orderDto.getId())) {
                isMoved = this.archiveOrder(orderDto);
                if (isMoved) {
                    iterator.remove();
                    System.out.println("removed: "+ orderDto.getId());
                }
            }
        }

        if (isMoved) {
            this.deleteOrderData(ids);
        }

    }

    @Transactional
    public void deleteOrderData(List<Integer> ids) {
        this.orderRepo.deleteODForIds(ids);
        this.orderRepo.deleteOrdersByIds(ids);
        System.out.print("deleted: ");
        for(Integer i : ids){
            System.out.print(i + ",");
        }
    }

    @Transactional
    public List<OrderDto> updateOrder(OrderDto orderDto, Integer orderId, Boolean flowUpdate) {
        List<OrderDto> lo = this.checkMap();
        Order order = (Order) this.orderRepo.findById(orderId).orElseThrow(() -> {
            return new ResourceNotFoundException("Order", "id", (long) orderId);
        });
        order.setDeliveryDate(orderDto.getDeliveryDate());
        order.setReferenceInfo(orderDto.getReferenceInfo());
        if (flowUpdate) {
            if (!order.hasOnlyOneDifference(this.dtoToOrder(orderDto))) {
                return new ArrayList();
            }

            this.updatingFlow(order, orderDto);
        }

        if (order.getCompleted() == null) {
            order.setCompleted("");
        }

        Order updatedOrder = (Order) this.orderRepo.save(order);
        OrderDto updatedOrderDto = this.orderToDto(updatedOrder);
        this.ordersMap.put(updatedOrderDto.getOrderNumber() + "," + updatedOrderDto.getRegel(), updatedOrderDto);
        boolean allOrdersComplete = this.ordersMap.values().stream().filter((ord) -> {
            return ord.getOrderNumber().equals(updatedOrderDto.getOrderNumber()) && updatedOrderDto.getId() != ord.getId();
        }).allMatch((ord) -> {
            return "C".equals(ord.getCompleted());
        });
        if (updatedOrder.getCompleted().equals("C") && allOrdersComplete) {
            List<Integer> idList = (List) this.ordersMap.values().stream().filter((ord) -> {
                return ord.getOrderNumber().equals(updatedOrderDto.getOrderNumber());
            }).map(OrderDto::getId).collect(Collectors.toList());
            this.moveToArchive(idList);
            this.orderDtoList = new ArrayList();
            Iterator var13 = this.ordersMap.entrySet().iterator();

            while (var13.hasNext()) {
                Map.Entry<String, OrderDto> entry = (Map.Entry) var13.next();
                OrderDto orderDto2 = (OrderDto) entry.getValue();
                orderDto2.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                this.orderDtoList.add(orderDto2);
            }

            return this.orderDtoList;
        } else {
            this.orderDtoList = new ArrayList();
            Iterator var9 = this.ordersMap.entrySet().iterator();

            while (var9.hasNext()) {
                Map.Entry<String, OrderDto> entry = (Map.Entry) var9.next();
                OrderDto orderDto2 = (OrderDto) entry.getValue();
                orderDto2.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                this.orderDtoList.add(orderDto2);
            }

            return this.orderDtoList;
        }
    }

    @Transactional
    public List<OrderDto> updateOrderColors(String orderNumber, String orderDep, String orderStatus, String flowVal) {
        List<OrderDto> lo = this.checkMap();
        String color = "";
        String prev = "";
        List<Integer> matchingIds = new ArrayList();
        Iterator var9 = this.ordersMap.values().iterator();

        while (var9.hasNext()) {
            OrderDto obj = (OrderDto) var9.next();
            if (obj.getOrderNumber().equals(orderNumber)) {
                matchingIds.add(obj.getId());
            }
        }

        String setterMethodName = "set" + orderDep.substring(0, 1).toUpperCase() + orderDep.substring(1);
        String getterMethodName = "get" + orderDep.substring(0, 1).toUpperCase() + orderDep.substring(1);

        try {
            Method setterMethod = OrderDto.class.getMethod(setterMethodName, String.class);
            Method getterMethod = OrderDto.class.getMethod(getterMethodName);
            Iterator var13 = this.ordersMap.keySet().iterator();

            label251:
            while (true) {
                String key;
                OrderDto orderDto;
                do {
                    if (!var13.hasNext()) {
                        break label251;
                    }

                    key = (String) var13.next();
                    orderDto = (OrderDto) this.ordersMap.get(key);
                } while (!orderDto.getOrderNumber().equals(orderNumber));

                if ("R".equals(getterMethod.invoke(orderDto)) && "R".equals(orderStatus) && (flowVal.equals("FWD") || flowVal.equals("RVS"))) {
                    setterMethod.invoke(orderDto, "Y");
                    color = "Y";
                } else if ("R".equals(getterMethod.invoke(orderDto)) && "R".equals(orderStatus) && flowVal.equals("HLT")) {
                    setterMethod.invoke(orderDto, "B");
                    color = "B";
                } else if ("Y".equals(getterMethod.invoke(orderDto)) && "Y".equals(orderStatus)) {
                    setterMethod.invoke(orderDto, "G");
                    color = "G";
                } else if ("B".equals(getterMethod.invoke(orderDto)) && "B".equals(orderStatus)) {
                    setterMethod.invoke(orderDto, "R");
                    color = "R";
                }

                List<OrderDepartment> depList = orderDto.getDepartments();
                Iterator var17 = depList.iterator();

                while (true) {
                    while (true) {
                        OrderDepartment dep;
                        do {
                            if (!var17.hasNext()) {
                                boolean allStatusGOrEmpty = orderDto.getDepartments().stream().allMatch((department) -> {
                                    return "G".equals(department.getStatus()) || "".equals(department.getStatus());
                                });
                                if (allStatusGOrEmpty) {
                                    orderDto.setCompleted("C");
                                }

                                this.ordersMap.put(key, orderDto);
                                continue label251;
                            }

                            dep = (OrderDepartment) var17.next();
                        } while (!orderDep.toUpperCase().equals(dep.getDepName()));

                        if ("R".equals(dep.getStatus()) && (flowVal.equals("FWD") || flowVal.equals("RVS"))) {
                            dep.setStatus("Y");
                            dep.setPrevStatus("R");
                            prev = dep.getPrevStatus();
                        } else if ("R".equals(dep.getStatus()) && flowVal.equals("HLT")) {
                            dep.setStatus("B");
                            dep.setPrevStatus("R");
                            prev = dep.getPrevStatus();
                        } else if ("Y".equals(dep.getStatus())) {
                            dep.setStatus("G");
                            dep.setPrevStatus("Y");
                            prev = dep.getPrevStatus();
                        } else if ("B".equals(dep.getStatus())) {
                            dep.setStatus("R");
                            dep.setPrevStatus("B");
                            prev = dep.getPrevStatus();
                        }
                    }
                }
            }
        } catch (Exception var19) {
            Exception e = var19;
            e.printStackTrace();
        }

        Iterator var24;
        Map.Entry entry;
        OrderDto orderDto;
        if (color.equals("Y")) {
            if (orderDep.equals("monLb")) {
                this.orderRepo.updateFieldForIdsMainmonLb(color, matchingIds);
            }

            if (orderDep.equals("monTr")) {
                this.orderRepo.updateFieldForIdsMainmonTr(color, matchingIds);
            }

            if (orderDep.equals("mwe")) {
                this.orderRepo.updateFieldForIdsMainmwe(color, matchingIds);
            }

            if (orderDep.equals("ser")) {
                this.orderRepo.updateFieldForIdsMainser(color, matchingIds);
            }

            if (orderDep.equals("exp")) {
                this.orderRepo.updateFieldForIdsMainexp(color, matchingIds);
            }

            this.orderRepo.updateOrderDepartmentStatusMain(color, prev, orderDep.toUpperCase(), matchingIds);
            this.orderDtoList = new ArrayList();
            var24 = this.ordersMap.entrySet().iterator();

            while (var24.hasNext()) {
                entry = (Map.Entry) var24.next();
                orderDto = (OrderDto) entry.getValue();
                orderDto.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                this.orderDtoList.add(orderDto);
            }

            return this.orderDtoList;
        } else if (color.equals("R")) {
            if (orderDep.equals("monLb")) {
                this.orderRepo.updateFieldForIdsMainmonLb(color, matchingIds);
            }

            if (orderDep.equals("monTr")) {
                this.orderRepo.updateFieldForIdsMainmonTr(color, matchingIds);
            }

            if (orderDep.equals("mwe")) {
                this.orderRepo.updateFieldForIdsMainmwe(color, matchingIds);
            }

            if (orderDep.equals("ser")) {
                this.orderRepo.updateFieldForIdsMainser(color, matchingIds);
            }

            if (orderDep.equals("exp")) {
                this.orderRepo.updateFieldForIdsMainexp(color, matchingIds);
            }

            this.orderRepo.updateOrderDepartmentStatusMain(color, prev, orderDep.toUpperCase(), matchingIds);
            this.orderDtoList = new ArrayList();
            var24 = this.ordersMap.entrySet().iterator();

            while (var24.hasNext()) {
                entry = (Map.Entry) var24.next();
                orderDto = (OrderDto) entry.getValue();
                orderDto.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                this.orderDtoList.add(orderDto);
            }

            return this.orderDtoList;
        } else if (color.equals("B")) {
            if (orderDep.equals("monLb")) {
                this.orderRepo.updateFieldForIdsMainmonLb(color, matchingIds);
            }

            if (orderDep.equals("monTr")) {
                this.orderRepo.updateFieldForIdsMainmonTr(color, matchingIds);
            }

            if (orderDep.equals("mwe")) {
                this.orderRepo.updateFieldForIdsMainmwe(color, matchingIds);
            }

            if (orderDep.equals("ser")) {
                this.orderRepo.updateFieldForIdsMainser(color, matchingIds);
            }

            if (orderDep.equals("exp")) {
                this.orderRepo.updateFieldForIdsMainexp(color, matchingIds);
            }

            this.orderRepo.updateOrderDepartmentStatusMain(color, prev, orderDep.toUpperCase(), matchingIds);
            this.orderDtoList = new ArrayList();
            var24 = this.ordersMap.entrySet().iterator();

            while (var24.hasNext()) {
                entry = (Map.Entry) var24.next();
                orderDto = (OrderDto) entry.getValue();
                orderDto.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                this.orderDtoList.add(orderDto);
            }

            return this.orderDtoList;
        } else if (color.equals("G")) {
            if (orderDep.equals("monLb")) {
                this.orderRepo.updateFieldForIdsMainmonLb(color, matchingIds);
            }

            if (orderDep.equals("monTr")) {
                this.orderRepo.updateFieldForIdsMainmonTr(color, matchingIds);
            }

            if (orderDep.equals("mwe")) {
                this.orderRepo.updateFieldForIdsMainmwe(color, matchingIds);
            }

            if (orderDep.equals("ser")) {
                this.orderRepo.updateFieldForIdsMainser(color, matchingIds);
            }

            if (orderDep.equals("exp")) {
                this.orderRepo.updateFieldForIdsMainexp(color, matchingIds);
            }

            this.orderRepo.updateOrderDepartmentStatusMain(color, prev, orderDep.toUpperCase(), matchingIds);
            long count = this.ordersMap.values().stream().filter((order) -> {
                return order != null && order.getOrderNumber() != null && order.getOrderNumber().equals(orderNumber) && order.getCompleted() != null && order.getCompleted().equals("C");
            }).count();
            Boolean allCompleted = count > 0L && count == this.ordersMap.values().stream().filter((order) -> {
                return order.getOrderNumber().equals(orderNumber);
            }).count();
            if (allCompleted) {
                this.moveToArchive(matchingIds);
            }

            this.orderDtoList = new ArrayList();
            Iterator var28 = this.ordersMap.entrySet().iterator();

            while (var28.hasNext()) {
                entry = (Map.Entry) var28.next();
                orderDto = (OrderDto) entry.getValue();
                orderDto.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                this.orderDtoList.add(orderDto);
            }

            return this.orderDtoList;
        } else {
            return new ArrayList();
        }
    }

    public void deleteOrder(Integer orderId) {
        Order order = (Order) this.orderRepo.findById(orderId).orElseThrow(() -> {
            return new ResourceNotFoundException("Order", "id", (long) orderId);
        });
        this.orderRepo.delete(order);
    }

    public OrderDto getOrderById(Integer orderId) {
        List<OrderDto> lo = this.checkMap();
        Order order = (Order) this.orderRepo.findById(orderId).orElseThrow(() -> {
            return new ResourceNotFoundException("User", "id", (long) orderId);
        });
        return this.orderToDto(order);
    }

    public List<OrderDto> getOrdersByUser(String user) {
        if (this.ordersMap.isEmpty()) {
            List<Order> ordersByUser = this.orderRepo.findByUser(user);
            List<OrderDto> orderDtos = (List) ordersByUser.stream().map((order) -> {
                return this.orderToDto(order);
            }).collect(Collectors.toList());
            return orderDtos;
        } else {
            this.orderDtoList = new ArrayList();
            Iterator var2 = this.ordersMap.entrySet().iterator();

            while (var2.hasNext()) {
                Map.Entry<String, OrderDto> entry = (Map.Entry) var2.next();
                if (((OrderDto) entry.getValue()).getUser().equals(user)) {
                    OrderDto orderDto = (OrderDto) entry.getValue();
                    this.orderDtoList.add(orderDto);
                }
            }

            return this.orderDtoList;
        }
    }

    public List<OrderDto> getAllOrders() {
        if (this.ordersMap.isEmpty()) {
            List<Order> allOrders = this.orderRepo.findAll();
            if (!allOrders.isEmpty() && allOrders != null) {
                List<OrderDto> orderDtos = (List) allOrders.stream().map((order) -> {
                    order.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                    OrderDto sortedDeps = this.orderToDto(order);
                    return sortedDeps;
                }).collect(Collectors.toList());
                Iterator var7 = orderDtos.iterator();

                while (var7.hasNext()) {
                    OrderDto orderDto = (OrderDto) var7.next();
                    this.ordersMap.put(orderDto.getOrderNumber() + "," + orderDto.getRegel(), orderDto);
                }

                return orderDtos;
            } else {
                return this.getCRMOrders();
            }
        } else {
            this.orderDtoList = new ArrayList();
            Iterator var1 = this.ordersMap.entrySet().iterator();

            while (var1.hasNext()) {
                Map.Entry<String, OrderDto> entry = (Map.Entry) var1.next();
                OrderDto orderDto = (OrderDto) entry.getValue();
                orderDto.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                this.orderDtoList.add(orderDto);
            }

            return this.orderDtoList;
        }
    }

    public List<OrderDto> checkMap() {
        if (this.ordersMap.isEmpty()) {
            List<Order> allOrders = this.orderRepo.findAll();
            if (!allOrders.isEmpty() && allOrders != null) {
                List<OrderDto> orderDtos = (List) allOrders.stream().map((order) -> {
                    order.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                    OrderDto sortedDeps = this.orderToDto(order);
                    return sortedDeps;
                }).collect(Collectors.toList());
                Iterator var3 = orderDtos.iterator();

                while (var3.hasNext()) {
                    OrderDto orderDto = (OrderDto) var3.next();
                    this.ordersMap.put(orderDto.getOrderNumber() + "," + orderDto.getRegel(), orderDto);
                }

                return orderDtos;
            } else {
                return this.getCRMOrders();
            }
        } else {
            List<OrderDto> mapOrders = new ArrayList(this.ordersMap.values());
            long dataLength = this.orderRepo.count();
            if (dataLength == (long) mapOrders.size()) {
                return null;
            } else {
                this.ordersMap.clear();
                List<Order> allOrders = this.orderRepo.findAll();
                List<OrderDto> orderDtos = (List) allOrders.stream().map((order) -> {
                    order.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                    OrderDto sortedDeps = this.orderToDto(order);
                    return sortedDeps;
                }).collect(Collectors.toList());
                Iterator var6 = orderDtos.iterator();

                while (var6.hasNext()) {
                    OrderDto orderDto = (OrderDto) var6.next();
                    this.ordersMap.put(orderDto.getOrderNumber() + "," + orderDto.getRegel(), orderDto);
                }

                this.orderDtoList = new ArrayList();
                var6 = this.ordersMap.entrySet().iterator();

                while (var6.hasNext()) {
                    Map.Entry<String, OrderDto> entry = (Map.Entry) var6.next();
                    OrderDto orderDto = (OrderDto) entry.getValue();
                    orderDto.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                    this.orderDtoList.add(orderDto);
                }

                return this.orderDtoList;
            }
        }
    }

    public void removingSameArchivedOrders(){
        List<Integer> matchingIds = this.ordersMap.values()
                .stream()
                .filter(order ->
                        this.archivedOrdersService.getAllArchivedOrders()
                                .stream()
                                .anyMatch(archivedOrder ->
                                        archivedOrder.getOrderNumber().equals(order.getOrderNumber())
                                )
                )
                .map(OrderDto::getId)
                .collect(Collectors.toList());
        System.out.println("maching ids: " + matchingIds.size());
        this.deleteOrderData(matchingIds);
    }

    @Async
    @Scheduled(fixedRate = 300000)
    public void runQuery() {
        if (!ordersMap.isEmpty() && !this.archivedOrdersService.getAllArchivedOrders().isEmpty()) {
            try {
                String driver = "sun.jdbc.odbc.JdbcOdbcDriver";
                System.out.println("----------driver----------");
                System.out.println(driver);
                String connectionString = "jdbc:odbc:DRIVER={Progress OpenEdge 11.7 driver};DSN=AGRPROD2;UID=ODBC;PWD=ODBC;HOST=W2K16DMBBU4;PORT=12501;DB=data;Trusted_Connection=Yes;";
                System.out.println("----------connectionString----------");
                System.out.println(connectionString);
                String query = "SELECT \"va-210\".\"cdorder\" AS 'Verkooporder', " +
                        "\"va-211\".\"cdprodukt\" AS 'Product' , \"va-211\".\"nrordrgl\" AS 'Regel'" +
                        "FROM DATA.PUB.\"va-210\" " +
                        "JOIN DATA.PUB.\"va-211\" ON \"va-210\".\"cdorder\" = \"va-211\".\"cdorder\" " +
                        "AND \"va-211\".\"cdadmin\" = \"va-210\".\"cdadmin\" " +
                        "WHERE (\"va-210\".\"cdstatus\" <> 'Z' And \"va-210\".\"cdstatus\" <> 'B') " +
                        "AND \"va-210\".\"cdadmin\" = '01' " +
                        "AND \"va-210\".\"cdvestiging\" = 'ree'";

                System.out.println(query);
                Class.forName(driver);
                try (Connection connection = DriverManager.getConnection(connectionString);
                     Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery(query)) {
                    if (statement != null) {
                        System.out.println("----------resultSet----------");
                        System.out.println(resultSet);
                        System.out.println(resultSet.next());
                        String orderNumber = null;

                        List<String> existingOrderNumbers = new ArrayList<>();
                        while (resultSet.next()) {
                            if (resultSet.wasNull()) {
                                System.out.println("no ordernumer");
                                continue;
                            }
                            orderNumber = resultSet.getString("Verkooporder");
                            String product = resultSet.getString("Product");
                            String regel = resultSet.getString("Regel");
                            existingOrderNumbers.add(orderNumber+","+regel);
//                            System.out.println("orderNumber");
//                            System.out.println(orderNumber);
                        }
//                        for (String on : existingOrderNumbers) {
//                            System.out.println(on);
//                        }
                        Set<String> uniqueones = new HashSet<>(existingOrderNumbers);
                        System.out.print("unique ones: ");
                        System.out.println(uniqueones.size());
                        System.out.println(existingOrderNumbers .size());
                        System.out.println(ordersMap.values().size());
                        List<OrderDto> orderList = this.ordersMap.values()
                                .stream()
                                .filter(ord -> {
                                    return !existingOrderNumbers.contains(ord.getOrderNumber() + "," + ord.getRegel());
                                })
                                .collect(Collectors.toList());

//                        for (Integer id : idList1) {
//                            System.out.println("id1: " + id);
//                        }
                        System.out.println("list1: " + orderList.size());
                        List<ArchivedOrdersDto> archivedOrdersList = this.archivedOrdersService.getAllArchivedOrders()
                                .stream()
                                .filter(ord -> {
                                    return !existingOrderNumbers.contains(ord.getOrderNumber() + "," + ord.getRegel());
                                })
                                .collect(Collectors.toList());
//                        for (Long id : idList2) {
//                            System.out.println("id2: " + id);
//                        }
                        System.out.println("list2: " + archivedOrdersList.size());
                        List<Integer> filteredOrders = orderList
                                .stream()
                                .filter(order ->
                                        archivedOrdersList.stream()
                                                .noneMatch(archivedOrder ->
                                                        archivedOrder.getOrderNumber().equals(order.getOrderNumber()) &&
                                                                archivedOrder.getRegel().equals(order.getRegel())
                                                )
                                ).map(OrderDto::getId)
                                .collect(Collectors.toList());

//                        for (Integer id : idList3) {
//                            System.out.println("id3: " + id);
//                        }
                        System.out.println("list3: " + filteredOrders.size());
                        this.moveToArchive(filteredOrders);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Transactional
    public List<OrderDto> getCRMOrders() {
        //{DataDirect 7.1 OpenEdge Wire Protocol};DSN=AGRPROD
        try {
            String driver = "sun.jdbc.odbc.JdbcOdbcDriver";
            System.out.println("----------driver----------");
            System.out.println(driver);
            String connectionString = "jdbc:odbc:DRIVER={Progress OpenEdge 11.7 driver};DSN=AGRPROD2;UID=ODBC;PWD=ODBC;HOST=W2K16DMBBU4;PORT=12501;DB=data;Trusted_Connection=Yes;";
            System.out.println("----------connectionString----------");
            System.out.println(connectionString);
            String query = "SELECT \"va-210\".\"cdorder\" AS 'Verkooporder', \"va-210\".\"cdordsrt\" AS 'Ordersoort', \"va-211\".\"cdborder\" AS 'Backorder', \"va-210\".\"cdgebruiker-init\" AS 'Gebruiker (I)', \"va-210\".\"cddeb\" AS 'Organisatie', \"ba-001\".\"naamorg\" AS 'Naam', \"ba-012\".\"postcode\" AS 'Postcode', \"ba-012\".\"plaats\" AS 'Plaats', \"ba-012\".\"cdland\" AS 'Land', \"va-210\".\"datum-lna\" AS 'Leverdatum', \"va-210\".\"opm-30\" AS 'Referentie', \"va-210\".\"datum-order\" AS 'Datum order', \"va-210\".\"SYS-DATE\" AS 'Datum laatste wijziging', \"va-210\".\"cdgebruiker\" AS 'Gebruiker (L)', \"va-211\".\"nrordrgl\" AS 'Regel', \"va-211\".\"aantbest\" AS 'Aantal besteld', \"va-211\".\"aanttelev\" AS 'Aantal geleverd', \"va-211\".\"cdprodukt\" AS 'Product', \"af-801\".\"tekst\" AS 'Omschrijving', \"va-211\".\"volgorde\" AS 'regelvolgorde', \"bb-043\".\"cdprodgrp\" FROM DATA.PUB.\"af-801\" , DATA.PUB.\"ba-001\" , DATA.PUB.\"ba-012\" , DATA.PUB.\"bb-043\" , DATA.PUB.\"va-210\" , DATA.PUB.\"va-211\" WHERE \"ba-001\".\"cdorg\" = \"va-210\".\"cdorg\" AND \"va-211\".\"cdadmin\" = \"va-210\".\"cdadmin\" AND \"va-211\".\"cdorder\" = \"va-210\".\"cdorder\" AND \"va-211\".\"cdorg\" = \"ba-001\".\"cdorg\" AND \"va-211\".\"cdprodukt\" = \"af-801\".\"cdsleutel1\" AND \"ba-012\".\"id-cdads\" = \"va-211\".\"id-cdads\" AND \"bb-043\".\"cdprodukt\" = \"va-211\".\"cdprodukt\" AND ((\"af-801\".\"cdtabel\"='bb-062') AND (\"va-210\".\"cdadmin\"='01') AND (\"va-211\".\"cdadmin\"='01') AND (\"va-210\".\"cdvestiging\"='ree') AND (\"va-210\".\"cdstatus\" <> 'Z' And \"va-210\".\"cdstatus\" <> 'B') AND (\"bb-043\".\"cdprodcat\"='pro'))";
            System.out.println("----------query----------");
            System.out.println(query);
            Class.forName(driver);
            try (Connection connection = DriverManager.getConnection(connectionString);
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {
                //Class.forName(driver);
                System.out.println("----------connection----------");
                System.out.println(connection);
                System.out.println("----------statement----------");
                System.out.println(statement);
                if (statement != null) {
                    System.out.println("----------resultSet----------");
                    System.out.println(resultSet);
                    String orderNumber = null;
                    while (resultSet.next()) {
                        orderNumber = resultSet.getString("Verkooporder");
                        if (resultSet.wasNull()) {
                            System.out.println("no ordernumer");
                            continue;
                        }
                        String orderType = resultSet.getString("Ordersoort");
                        String backOrder = resultSet.getString("Backorder");
                        String user = resultSet.getString("Gebruiker (I)");
                        String organization = resultSet.getString("Organisatie");
                        String customerName = resultSet.getString("Naam");
                        String postCode = resultSet.getString("Postcode");
                        String city = resultSet.getString("Plaats");
                        String country = resultSet.getString("Land");
                        String deliveryDate = resultSet.getString("Leverdatum");
                        String referenceInfo = resultSet.getString("Referentie");
                        String creationDate = resultSet.getString("Datum order");
                        String modificationDate = resultSet.getString("Datum laatste wijziging");
                        String verifierUser = resultSet.getString("Gebruiker (L)");
                        String regel = resultSet.getString("Regel");
                        String aantal = resultSet.getString("Aantal besteld");
                        String product = resultSet.getString("Product");
                        String omsumin = resultSet.getString("Omschrijving");
                        String cdProdGrp = resultSet.getString("cdprodgrp");
                        String deliveryDate2 = "";
                        OrderDto orderDto;
                        String finalOrderNumber = orderNumber;
                        if (!this.ordersMap.containsKey(orderNumber + "," + regel) && !this.archivedOrdersService.getAllArchivedOrders().stream().anyMatch((obj) -> {
                            return obj.getOrderNumber().equals(finalOrderNumber);
                        })) {
                            orderDto = new OrderDto();
                            String finalOrderNumber1 = orderNumber;
                            if (!this.ordersMap.entrySet().stream().anyMatch((obj) -> {
                                return ((OrderDto) obj.getValue()).getOrderNumber().equals(finalOrderNumber1);
                            })) {
                                orderDto.setIsParent(1);
                            } else {
                                orderDto.setIsParent(0);
                            }

                            int maxId = this.ordersMap.values().stream().mapToInt(OrderDto::getId).max().orElse(0);
                            ++maxId;
                            orderDto.setId(maxId);
                            orderDto.setOrderNumber(orderNumber);
                            orderDto.setOrderType(orderType);
                            orderDto.setBackOrder(backOrder);
                            orderDto.setCdProdGrp(cdProdGrp);
                            this.settingUpFlow(orderDto);
                            orderDto.setUser(user);
                            orderDto.setOrganization(organization);
                            orderDto.setCustomerName(customerName);
                            orderDto.setPostCode(postCode);
                            orderDto.setCity(city);
                            orderDto.setCountry(country);
                            if (deliveryDate == null) {
                                orderDto.setDeliveryDate("");
                            } else {
                                orderDto.setDeliveryDate(deliveryDate);
                            }

                            orderDto.setReferenceInfo(referenceInfo);
                            orderDto.setCreationDate(creationDate);
                            orderDto.setModificationDate(modificationDate);
                            orderDto.setVerifierUser(verifierUser);
                            orderDto.setRegel(regel);
                            orderDto.setAantal(aantal);
                            orderDto.setProduct(product);
                            orderDto.setOmsumin(omsumin);
                            deliveryDate2 = orderDto.getDeliveryDate();
                            if (!this.createOrder(orderDto)) {
                                System.out.println("Failed to create record in app");
                            } else {
                                this.ordersMap.put(orderNumber + "," + regel, orderDto);
                            }
                        }

                        if (this.ordersMap.containsKey(orderNumber + "," + regel) && !((OrderDto) this.ordersMap.get(orderNumber + "," + regel)).getDeliveryDate().equals(deliveryDate2) && !deliveryDate2.equals("")) {
                            orderDto = (OrderDto) this.ordersMap.get(orderNumber + "," + regel);
                            orderDto.setDeliveryDate(deliveryDate2);
                            orderDto.setReferenceInfo(referenceInfo);
                            this.updateOrder(orderDto, ((OrderDto) this.ordersMap.get(orderNumber + "," + regel)).getId(), false);
                        }
                    }
                }
            } catch (SQLException var35) {
                var35.printStackTrace();
                List<OrderDto> orderList = this.getAllOrders();
                this.orderDtoList = orderList;
                return this.orderDtoList;
                //new ResourceNotFoundException("Order", "CRM", "N/A");
                //return null;
            } catch (Exception var37) {
                Exception e = var37;
                e.printStackTrace();
                List<OrderDto> orderList = this.getAllOrders();
                this.orderDtoList = orderList;
                return this.orderDtoList;
                //new ResourceNotFoundException("Order", "CRM", "N/A");
                //return null;
            }

            this.ordersMap.clear();
            List<OrderDto> orderList = this.getAllOrders();
            this.orderDtoList = orderList;
            return this.orderDtoList;
        } catch (Exception var39) {
            Exception e = var39;
            e.printStackTrace();
            List<OrderDto> orderList = this.getAllOrders();
            this.orderDtoList = orderList;
            return this.orderDtoList;
            //new ResourceNotFoundException("Order", "CRM", "N/A");
            //return null;
        }
    }

    @Transactional
    private void settingUpFlow(OrderDto orderDto) {
        String orderType = orderDto.getOrderType();
        List<OrderDepartment> depList = new ArrayList();
        String wheelOrder = orderDto.getCdProdGrp();
        String pattern = "(182|183|184|440|820|821|822|823|824|825|826|850|851)";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(wheelOrder);
        if (matcher.find()) {
            orderDto.setSme("");
            orderDto.setSpu("");
            depList.add(new OrderDepartment(2, "SME", "", this.dtoToOrder(orderDto)));
            depList.add(new OrderDepartment(3, "SPU", "", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("LOS")) {
            orderDto.setTra("R");
            depList.add(new OrderDepartment(8, "TRA", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("LAS")) {
            orderDto.setExp("R");
            depList.add(new OrderDepartment(9, "EXP", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("LSO")) {
            orderDto.setSer("R");
            depList.add(new OrderDepartment(7, "SER", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("MAO")) {
            orderDto.setMonLb("R");
            orderDto.setExp("R");
            depList.add(new OrderDepartment(4, "MONLB", "R", this.dtoToOrder(orderDto)));
            depList.add(new OrderDepartment(9, "EXP", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("MLO")) {
            orderDto.setMonLb("R");
            orderDto.setTra("R");
            depList.add(new OrderDepartment(4, "MONLB", "R", this.dtoToOrder(orderDto)));
            depList.add(new OrderDepartment(8, "TRA", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("MWO")) {
            orderDto.setMwe("R");
            depList.add(new OrderDepartment(6, "MWE", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("MSO")) {
            orderDto.setMonLb("R");
            orderDto.setSer("R");
            depList.add(new OrderDepartment(4, "MONLB", "R", this.dtoToOrder(orderDto)));
            depList.add(new OrderDepartment(7, "SER", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("MLT")) {
            orderDto.setMonTr("R");
            orderDto.setTra("R");
            depList.add(new OrderDepartment(5, "MONTR", "R", this.dtoToOrder(orderDto)));
            depList.add(new OrderDepartment(8, "TRA", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("MST")) {
            orderDto.setMonTr("R");
            orderDto.setSer("R");
            depList.add(new OrderDepartment(5, "MONTR", "R", this.dtoToOrder(orderDto)));
            depList.add(new OrderDepartment(7, "SER", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("LOP")) {
            orderDto.setTra("R");
            depList.add(new OrderDepartment(8, "TRA", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("LAP")) {
            orderDto.setExp("R");
            depList.add(new OrderDepartment(9, "EXP", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("LSP")) {
            orderDto.setSer("R");
            depList.add(new OrderDepartment(7, "SER", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("MAP")) {
            orderDto.setMonLb("R");
            orderDto.setExp("R");
            depList.add(new OrderDepartment(4, "MONLB", "R", this.dtoToOrder(orderDto)));
            depList.add(new OrderDepartment(9, "EXP", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("MLP")) {
            orderDto.setMonLb("R");
            orderDto.setTra("R");
            depList.add(new OrderDepartment(4, "MONLB", "R", this.dtoToOrder(orderDto)));
            depList.add(new OrderDepartment(8, "TRA", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("MWP")) {
            orderDto.setMwe("R");
            depList.add(new OrderDepartment(6, "MWE", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("MSP")) {
            orderDto.setMonLb("R");
            orderDto.setSer("R");
            depList.add(new OrderDepartment(4, "MONLB", "R", this.dtoToOrder(orderDto)));
            depList.add(new OrderDepartment(7, "SER", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("MSE")) {
            orderDto.setMonTr("R");
            orderDto.setSer("R");
            depList.add(new OrderDepartment(5, "MONTR", "R", this.dtoToOrder(orderDto)));
            depList.add(new OrderDepartment(7, "SER", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("MLE")) {
            orderDto.setMonTr("R");
            orderDto.setTra("R");
            depList.add(new OrderDepartment(5, "MONTR", "R", this.dtoToOrder(orderDto)));
            depList.add(new OrderDepartment(8, "TRA", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("WEB")) {
            orderDto.setTra("R");
            depList.add(new OrderDepartment(8, "TRA", "R", this.dtoToOrder(orderDto)));
        }

        if (orderType.equals("BBA")) {
            orderDto.setTra("R");
            depList.add(new OrderDepartment(8, "TRA", "R", this.dtoToOrder(orderDto)));
        }

        orderDto.setDepartments(depList);
    }

    private void updatingFlow(Order order, OrderDto orderDto) {
        String orderTypeDto = orderDto.getOrderType();
        String orderType = order.getOrderType();
        List<OrderDepartment> depList = order.getDepartments();
        List<OrderDepartment> depListDto = orderDto.getDepartments();
        if (depList != null) {
            depList.sort(Comparator.comparingInt(OrderDepartment::getDepId));
        }

        if (orderDto.getCompleted() == null) {
            orderDto.setCompleted("");
        }

        if (orderDto.getCompleted().equals("C")) {
            order.setCompleted("C");
        }

        int index;
        if (orderType.equals(orderTypeDto) && orderType.equals("LOS")) {
            order.setTra(orderDto.getTra());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 8;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getTra())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getTra());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("LAS")) {
            order.setExp(orderDto.getExp());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 9;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getExp())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getExp());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("LSO")) {
            order.setSer(orderDto.getSer());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 7;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getSer())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getSer());
                }
            }
        }

        int index2;
        if (orderType.equals(orderTypeDto) && orderType.equals("MAO")) {
            order.setMonLb(orderDto.getMonLb());
            order.setExp(orderDto.getExp());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 4;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getMonLb())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getMonLb());
                }
            }

            index2 = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 9;
            }).findFirst().orElse(-1);
            if (index2 != -1) {
                if (((OrderDepartment) depList.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index2)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index2)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index2)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus(((OrderDepartment) depList.get(index2)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index2)).getStatus().equals(orderDto.getExp())) {
                    ((OrderDepartment) depList.get(index2)).setStatus(orderDto.getExp());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("MLO")) {
            order.setMonLb(orderDto.getMonLb());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 4;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getMonLb())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getMonLb());
                }
            }

            index2 = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 8;
            }).findFirst().orElse(-1);
            if (index2 != -1) {
                if (((OrderDepartment) depList.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index2)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index2)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index2)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus(((OrderDepartment) depList.get(index2)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index2)).getStatus().equals(orderDto.getTra())) {
                    ((OrderDepartment) depList.get(index2)).setStatus(orderDto.getTra());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("MWO")) {
            order.setMwe(orderDto.getMwe());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 6;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getMwe())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getMwe());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("MSO")) {
            order.setMonLb(orderDto.getMonLb());
            order.setSer(orderDto.getSer());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 4;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getMonLb())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getMonLb());
                }
            }

            index2 = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 7;
            }).findFirst().orElse(-1);
            if (index2 != -1) {
                if (((OrderDepartment) depList.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index2)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index2)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index2)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus(((OrderDepartment) depList.get(index2)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index2)).getStatus().equals(orderDto.getSer())) {
                    ((OrderDepartment) depList.get(index2)).setStatus(orderDto.getSer());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("MLT")) {
            order.setMonTr(orderDto.getMonTr());
            order.setTra(orderDto.getTra());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 5;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getMonTr())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getMonTr());
                }
            }

            index2 = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 8;
            }).findFirst().orElse(-1);
            if (index2 != -1) {
                if (((OrderDepartment) depList.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index2)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index2)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index2)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus(((OrderDepartment) depList.get(index2)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index2)).getStatus().equals(orderDto.getTra())) {
                    ((OrderDepartment) depList.get(index2)).setStatus(orderDto.getTra());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("MST")) {
            order.setMonTr(orderDto.getMonTr());
            order.setSer(orderDto.getSer());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 5;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getMonTr())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getMonTr());
                }
            }

            index2 = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 7;
            }).findFirst().orElse(-1);
            if (index2 != -1) {
                if (((OrderDepartment) depList.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index2)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index2)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index2)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus(((OrderDepartment) depList.get(index2)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index2)).getStatus().equals(orderDto.getSer())) {
                    ((OrderDepartment) depList.get(index2)).setStatus(orderDto.getSer());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("LOP")) {
            order.setTra(orderDto.getTra());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 8;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getTra())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getTra());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("LAP")) {
            order.setExp(orderDto.getExp());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 9;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getExp())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getExp());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("LSP")) {
            order.setSer(orderDto.getSer());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 7;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getSer())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getSer());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("MAP")) {
            order.setMonLb(orderDto.getMonLb());
            order.setExp(orderDto.getExp());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 4;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getMonLb())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getMonLb());
                }
            }

            index2 = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 9;
            }).findFirst().orElse(-1);
            if (index2 != -1) {
                if (((OrderDepartment) depList.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index2)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index2)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index2)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus(((OrderDepartment) depList.get(index2)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index2)).getStatus().equals(orderDto.getExp())) {
                    ((OrderDepartment) depList.get(index2)).setStatus(orderDto.getExp());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("MLP")) {
            order.setMonLb(orderDto.getMonLb());
            order.setTra(orderDto.getTra());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 4;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getMonLb())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getMonLb());
                }
            }

            index2 = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 8;
            }).findFirst().orElse(-1);
            if (index2 != -1) {
                if (((OrderDepartment) depList.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index2)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index2)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index2)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus(((OrderDepartment) depList.get(index2)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index2)).getStatus().equals(orderDto.getTra())) {
                    ((OrderDepartment) depList.get(index2)).setStatus(orderDto.getTra());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("MWP")) {
            order.setMwe(orderDto.getMwe());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 6;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getMwe())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getMwe());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("MSP")) {
            order.setMonLb(orderDto.getMonLb());
            order.setSer(orderDto.getSer());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 4;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getMonLb())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getMonLb());
                }
            }

            index2 = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 7;
            }).findFirst().orElse(-1);
            if (index2 != -1) {
                if (((OrderDepartment) depList.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index2)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index2)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index2)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus(((OrderDepartment) depList.get(index2)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index2)).getStatus().equals(orderDto.getSer())) {
                    ((OrderDepartment) depList.get(index2)).setStatus(orderDto.getSer());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("MSE")) {
            order.setMonTr(orderDto.getMonTr());
            order.setSer(orderDto.getSer());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 5;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getMonTr())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getMonTr());
                }
            }

            index2 = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 7;
            }).findFirst().orElse(-1);
            if (index2 != -1) {
                if (((OrderDepartment) depList.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index2)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index2)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index2)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus(((OrderDepartment) depList.get(index2)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index2)).getStatus().equals(orderDto.getSer())) {
                    ((OrderDepartment) depList.get(index2)).setStatus(orderDto.getSer());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("MLE")) {
            order.setMonTr(orderDto.getMonTr());
            order.setTra(orderDto.getTra());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 5;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getMonTr())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getMonTr());
                }
            }

            index2 = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 8;
            }).findFirst().orElse(-1);
            if (index2 != -1) {
                if (((OrderDepartment) depList.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index2)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index2)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index2)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index2)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index2)).setPrevStatus(((OrderDepartment) depList.get(index2)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index2)).getStatus().equals(orderDto.getTra())) {
                    ((OrderDepartment) depList.get(index2)).setStatus(orderDto.getTra());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("WEB")) {
            order.setTra(orderDto.getTra());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 8;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getTra())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getTra());
                }
            }
        }

        if (orderType.equals(orderTypeDto) && orderType.equals("BBA")) {
            order.setTra(orderDto.getTra());
            index = IntStream.range(0, depList.size()).filter((i) -> {
                return ((OrderDepartment) depList.get(i)).getDepId() == 8;
            }).findFirst().orElse(-1);
            if (index != -1) {
                if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                }

                if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                    ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                }

                if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                    ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                }

                if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getTra())) {
                    ((OrderDepartment) depList.get(index)).setStatus(orderDto.getTra());
                }
            }
        }

        if (order.getExclamation() == null) {
            order.setExclamation("");
        }

        if (orderDto.getExclamation() == null) {
            orderDto.setExclamation("");
        }

        if (order.getExcNote() == null) {
            order.setExcNote("");
        }

        if (orderDto.getExcNote() == null) {
            orderDto.setExcNote("");
        }

        if (order.getExclamation().equals("JA") && orderDto.getExclamation().equals("NEE")) {
            order.setExclamation("NEE");
            order.setExcNote("");
        }

        if ((order.getExclamation().equals("NEE") || order.getExclamation().equals("")) && orderDto.getExclamation().equals("JA")) {
            order.setExclamation("JA");
            order.setExcNote(orderDto.getExcNote());
        }

        if (depList != null) {
            if (orderDto.getBackOrder() == null) {
                orderDto.setBackOrder("");
            }

            if (orderDto.getBackOrder() != null && !orderDto.getBackOrder().equals("") && !orderDto.getBackOrder().equals("O")) {
                order.setBackOrder(orderDto.getBackOrder());
            } else if ((orderDto.getBackOrder() == null || orderDto.getBackOrder().equals("") || orderDto.getBackOrder().equals("O")) && (order.getBackOrder() != null || !order.getBackOrder().equals("") || !order.getBackOrder().equals("O"))) {
                order.setBackOrder("O");
            }

            if (orderDto.getSme() != null) {
                if (orderDto.getSme() == null && orderDto.getSme() == "") {
                    if ((orderDto.getSme() == null || orderDto.getSme().equals("")) && (order.getSme() != null || !order.getSme().equals(""))) {
                        index = IntStream.range(0, depList.size()).filter((i) -> {
                            return ((OrderDepartment) depList.get(i)).getDepId() == 2;
                        }).findFirst().orElse(-1);
                        if (index != -1) {
                            if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                                ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                            }

                            if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                                ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                            }

                            if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                                ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                            }

                            if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getSme())) {
                                ((OrderDepartment) depList.get(index)).setStatus(orderDto.getSme());
                            }
                        }
                    }
                } else {
                    order.setSme(orderDto.getSme());
                    if (!depList.stream().anyMatch((dep) -> {
                        return dep.getDepId() == 2;
                    })) {
                        if (!depList.stream().anyMatch((dep) -> {
                            return dep.getDepId() == 1;
                        })) {
                            depList.add(0, new OrderDepartment(2, "SME", "R", this.dtoToOrder(orderDto)));
                        } else {
                            depList.add(1, new OrderDepartment(2, "SME", "R", this.dtoToOrder(orderDto)));
                        }
                    } else {
                        index = IntStream.range(0, depList.size()).filter((i) -> {
                            return ((OrderDepartment) depList.get(i)).getDepId() == 2;
                        }).findFirst().orElse(-1);
                        if (index != -1) {
                            if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                                ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                            }

                            if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                                ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                            }

                            if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                                ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                            }

                            if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getSme())) {
                                ((OrderDepartment) depList.get(index)).setStatus(orderDto.getSme());
                            }
                        }
                    }
                }

                if (orderDto.getSpu() == null) {
                    orderDto.setSpu("");
                }

                if (orderDto.getSpu() == null && orderDto.getSpu() == "") {
                    if ((orderDto.getSpu() == null || orderDto.getSpu().equals("")) && (order.getSme() != null || !order.getSme().equals(""))) {
                        index = IntStream.range(0, depList.size()).filter((i) -> {
                            return ((OrderDepartment) depList.get(i)).getDepId() == 3;
                        }).findFirst().orElse(-1);
                        if (index != -1) {
                            if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                                ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                            }

                            if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                                ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                            }

                            if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                                ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                            }

                            if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getSpu())) {
                                ((OrderDepartment) depList.get(index)).setStatus(orderDto.getSpu());
                            }
                        }
                    }
                } else {
                    order.setSpu(orderDto.getSpu());
                    if (!depList.stream().anyMatch((dep) -> {
                        return dep.getDepId() == 3;
                    })) {
                        if (!depList.stream().anyMatch((dep) -> {
                            return dep.getDepId() == 1;
                        }) && !depList.stream().anyMatch((dep) -> {
                            return dep.getDepId() == 2;
                        })) {
                            depList.add(0, new OrderDepartment(3, "SPU", "R", this.dtoToOrder(orderDto)));
                        } else if (depList.stream().anyMatch((dep) -> {
                            return dep.getDepId() == 1;
                        }) && depList.stream().anyMatch((dep) -> {
                            return dep.getDepId() == 2;
                        })) {
                            depList.add(2, new OrderDepartment(3, "SPU", "R", this.dtoToOrder(orderDto)));
                        } else if (depList.stream().anyMatch((dep) -> {
                            return dep.getDepId() == 1;
                        }) || depList.stream().anyMatch((dep) -> {
                            return dep.getDepId() == 2;
                        })) {
                            depList.add(1, new OrderDepartment(3, "SPU", "R", this.dtoToOrder(orderDto)));
                        }
                    } else {
                        index = IntStream.range(0, depList.size()).filter((i) -> {
                            return ((OrderDepartment) depList.get(i)).getDepId() == 3;
                        }).findFirst().orElse(-1);
                        if (index != -1) {
                            if (((OrderDepartment) depList.get(index)).getPrevStatus() == null) {
                                ((OrderDepartment) depList.get(index)).setPrevStatus("n");
                            }

                            if (((OrderDepartment) depListDto.get(index)).getPrevStatus() == null) {
                                ((OrderDepartment) depListDto.get(index)).setPrevStatus("n");
                            }

                            if (!((OrderDepartment) depList.get(index)).getPrevStatus().equals(((OrderDepartment) depListDto.get(index)).getPrevStatus())) {
                                ((OrderDepartment) depList.get(index)).setPrevStatus(((OrderDepartment) depList.get(index)).getStatus());
                            }

                            if (!((OrderDepartment) depList.get(index)).getStatus().equals(orderDto.getSpu())) {
                                ((OrderDepartment) depList.get(index)).setStatus(orderDto.getSpu());
                            }
                        }
                    }
                }
            }
        }

        order.setDepartments(depList);
        boolean allStatusGOrEmpty = orderDto.getDepartments().stream().allMatch((department) -> {
            return "G".equals(department.getStatus()) || "".equals(department.getStatus());
        });
        if (allStatusGOrEmpty) {
            order.setCompleted("C");
            orderDto.setCompleted("C");
        }

    }

    @Transactional
    public Boolean updateTraColors(String ids, Long entryId) {
        String color = "";
        String prev = "";
        List<Integer> idList = new ArrayList();
        String[] idArray = ids.split(",");
        String[] var7 = idArray;
        int var8 = idArray.length;

        for (int var9 = 0; var9 < var8; ++var9) {
            String id = var7[var9];
            String trimmedId = id.trim();
            if (!trimmedId.isEmpty()) {
                int parsedId = Integer.parseInt(trimmedId);
                idList.add(parsedId);
            }
        }

        Iterator var13 = this.ordersMap.keySet().iterator();

        while (true) {
            String key;
            OrderDto orderDto;
            do {
                if (!var13.hasNext()) {
                    if (color.equals("Y")) {
                        this.orderRepo.updateFieldForRIds(color, idList);
                        this.orderRepo.updateOrderDepartmentStatusR(color, prev, idList);
                        return true;
                    }

                    if (color.equals("G")) {
                        this.orderRepo.updateFieldForYIds(color, idList);
                        this.orderRepo.updateOrderDepartmentStatusY(color, prev, idList);
                        this.moveToArchive(idList);
                        return true;
                    }

                    return false;
                }

                key = (String) var13.next();
                orderDto = (OrderDto) this.ordersMap.get(key);
            } while (!idList.contains(orderDto.getId()));

            if ("R".equals(orderDto.getTra())) {
                orderDto.setTra("Y");
                color = "Y";
            } else if ("Y".equals(orderDto.getTra())) {
                orderDto.setTra("G");
                color = "G";
            }

            List<OrderDepartment> depList = orderDto.getDepartments();
            Iterator var17 = depList.iterator();

            while (var17.hasNext()) {
                OrderDepartment dep = (OrderDepartment) var17.next();
                if ("TRA".equals(dep.getDepName())) {
                    if ("R".equals(dep.getStatus())) {
                        dep.setStatus("Y");
                        dep.setPrevStatus("R");
                        prev = dep.getPrevStatus();
                    } else if ("Y".equals(dep.getStatus())) {
                        dep.setStatus("G");
                        dep.setPrevStatus("Y");
                        prev = dep.getPrevStatus();
                    }
                }
            }

            boolean allStatusGOrEmpty = orderDto.getDepartments().stream().allMatch((department) -> {
                return "G".equals(department.getStatus()) || "".equals(department.getStatus());
            });
            if (allStatusGOrEmpty) {
                orderDto.setCompleted("C");
            }

            this.ordersMap.put(key, orderDto);
        }
    }

    public Order dtoToOrder(OrderDto orderDto) {
        Order order = (Order) this.modelMapper.map(orderDto, Order.class);
        return order;
    }

    public OrderDto orderToDto(Order order) {
        OrderDto orderDto = (OrderDto) this.modelMapper.map(order, OrderDto.class);
        return orderDto;
    }
}