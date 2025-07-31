
package com.web.appts.services.imp;

import com.web.appts.DTO.*;
import com.web.appts.entities.*;
import com.web.appts.exceptions.ResourceNotFoundException;
import com.web.appts.repositories.*;
import com.web.appts.services.ArchivedOrdersService;
import com.web.appts.services.OrderService;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.transaction.Transactional;

import com.web.appts.utils.OdbcConnectionMonitor;
import com.web.appts.utils.OrderType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.StaleStateException;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static java.util.Collections.min;

@Service
public class OrderServiceImp implements OrderService {
    @Autowired
    private ConfigurableApplicationContext configurableContext;

    Map<String, OrderDto> ordersMap = new HashMap();
    Map<String, OrderDto> archivedOrdersMap = new HashMap();
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImp.class);
    List<OrderDto> orderDtoList;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private OrderRepo orderRepo;
    @Autowired
    private CustomOrderRepo customOrderRepo;
    @Autowired
    TransportOrderLinesRepo transportOrderLinesRepo;
    @Autowired
    private MonSubOrdersRepo monSubOrdersRepo;
    @Autowired
    private ArchivedOrdersService archivedOrdersService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OdbcConnectionMonitor connectionMonitor;
    private static final String DB_URL = "jdbc:odbc:DRIVER={Progress OpenEdge 11.7 driver};DSN=AGRPROD2;UID=ODBC;PWD=ODBC;HOST=W2K16DMBBU4;PORT=12501;DB=data;FetchSize=100;PacketSize=4096;";
    private static final String USERNAME = "ODBC";
    private static final String PASSWORD = "ODBC";
    private static Connection connection;
    private static final AtomicBoolean inUse = new AtomicBoolean(false);

    private final Set<Connection> activeConnections = new HashSet<>();

    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;

//    public Connection getConnection() throws SQLException {
//        logger.info("getConnection() called.");
//        Connection connection = secondaryDataSource.getConnection();
//        logger.info("Connection acquired: " + connection);
//        activeConnections.add(connection);
//        return connection;
//    }

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            logger.info("creating new connection");
            connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            connection.setAutoCommit(true);
        }
        return connection;
    }

    // Mark connection as in use (prevents unnecessary closures)
    public static boolean tryAcquire() {
        return inUse.compareAndSet(false, true);
    }

    // Mark connection as released
    public static void release() {
        inUse.set(false);
    }

    // Close the connection manually
    public static synchronized void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void closeConnections() {
        logger.info("Closing connections. Total active: " + activeConnections.size());

        Iterator<Connection> iterator = activeConnections.iterator();
        while (iterator.hasNext()) {
            Connection connection = iterator.next();
            try {
                logger.info("connection is null? " + connection);
                if (connection != null) {
                    logger.info("connection is closed? " + connection.isClosed());
                }
                if (connection != null && !connection.isClosed()) {
                    logger.info("Closing connection: " + connection);
                    connection.close();
                    logger.info("Connection closed: " + connection);
                }
            } catch (SQLException e) {
                logger.error("Error closing connection: " + connection, e);
            } finally {
                iterator.remove(); // Remove from list even if it fails to close
            }
        }

        logger.info("All connections closed. Active connections now: " + activeConnections.size());
    }

    @Async
    @Scheduled(fixedRate = 200000)
    protected void keepConnectionAlive() {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT count(*) AS CNT FROM \"sysprogress\".\"syscalctable\"");
            getConnection().clearWarnings();

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                logger.info("ODBC connection is active.");
            }
        } catch (SQLException e) {
            logger.warn("Connection check failed: " + e.getMessage());
        }
    }


    @Autowired
    @Lazy
    private ApplicationContext context;

    public OrderServiceImp() {
    }

    private void forceCloseODBCConnections() {
        if (secondaryDataSource == null) {
            logger.warn("Secondary DataSource is null. Skipping force close.");
            return;
        }

        try (Connection connection = secondaryDataSource.getConnection()) {
            if (connection != null && !connection.isClosed()) {
                logger.info("Forced closure of secondaryDataSource connection.");
            }
        } catch (SQLException e) {
            logger.warn("Error while force closing ODBC connection: " + e.getMessage());
        }
    }


    private void refreshDataSource() {
        try {
            closeConnections();
//            forceCloseODBCConnections();

            ConfigurableListableBeanFactory beanFactory = configurableContext.getBeanFactory();

            if (beanFactory.containsBean("secondaryDataSource")) {
                beanFactory.destroyBean("secondaryDataSource");
            }

            logger.info("bean destroyed");
            secondaryDataSource = null;
            logger.info("sds made null");

            System.gc();

            Thread.sleep(2000);


            secondaryDataSource = configurableContext.getBean("secondaryDataSource", DataSource.class);

            logger.info("DataSource refreshed successfully!");

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.info(ex.getCause().getMessage());
            logger.info(ex.getMessage());
            logger.info(ex.getCause().getLocalizedMessage());
            logger.info("Failed to refresh DataSource.");
        }
    }

    private void reloadJDBCDriver() {
        try {
            closeConnections();

            // Log all drivers before deregistering
            Enumeration<Driver> driversBefore = DriverManager.getDrivers();
            logger.info("Drivers before deregistration:");
            while (driversBefore.hasMoreElements()) {
                Driver driver = driversBefore.nextElement();
                logger.info(" - " + driver);
            }

            // Remove ODBC driver
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                if (driver.getClass().getName().equals("sun.jdbc.odbc.JdbcOdbcDriver")) {
                    DriverManager.deregisterDriver(driver);
                    System.gc(); // Hint JVM to clean up resources
                    logger.info("Deregistered JDBC-ODBC driver: " + driver);
                }
            }

            // Log all drivers after deregistration
            Enumeration<Driver> driversAfter = DriverManager.getDrivers();
            logger.info("Drivers after deregistration:");
            while (driversAfter.hasMoreElements()) {
                Driver driver = driversAfter.nextElement();
                logger.info(" - " + driver);
            }
            System.gc();
            Thread.sleep(2000);

            // Reload driver
            Class<?> driverClass = Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
            Driver driverInstance = (Driver) driverClass.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(driverInstance);
            logger.info("Re-registered JDBC-ODBC driver: " + driverInstance);

            // Confirm driver is reloaded
            Enumeration<Driver> driversFinal = DriverManager.getDrivers();
            logger.info("Drivers after re-registration:");
            while (driversFinal.hasMoreElements()) {
                Driver driver = driversFinal.nextElement();
                logger.info(" - " + driver);
            }

        } catch (Exception e) {
            logger.error("Failed to reload JDBC-ODBC Driver.", e);
        }
    }


//    private void reloadJDBCDriver() {
//        try {
//            closeConnections();
//            Enumeration<Driver> drivers = DriverManager.getDrivers();
//            while (drivers.hasMoreElements()) {
//                Driver driver = drivers.nextElement();
//                if (driver.getClass().getName().equals("sun.jdbc.odbc.JdbcOdbcDriver")) {
//                    DriverManager.deregisterDriver(driver);
//                    System.gc();
//                    logger.info("Deregistered JDBC-ODBC driver: " + driver);
//                    break;
//                }
//            }
//
//            Enumeration<Driver> drivers2 = DriverManager.getDrivers();
//            while (drivers2.hasMoreElements()) {
//                Driver driver = drivers2.nextElement();
//                if (driver.getClass().getName().equals("sun.jdbc.odbc.JdbcOdbcDriver")) {
//                    logger.info("JDBC-ODBC driver: " + driver);
//                    break;
//                }
//            }
//            logger.info("JDBC-ODBC driver2 ");
//
//            Thread.sleep(1000);
//            Class<?> driverClass = Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
//            Driver driverInstance = (Driver) driverClass.getDeclaredConstructor().newInstance();
//            DriverManager.registerDriver(driverInstance);
//
//            Enumeration<Driver> drivers3 = DriverManager.getDrivers();
//            while (drivers3.hasMoreElements()) {
//                Driver driver = drivers3.nextElement();
//                if (driver.getClass().getName().equals("sun.jdbc.odbc.JdbcOdbcDriver")) {
//                    logger.info("JDBC-ODBC driver: " + driver);
//                    break;
//                }
//            }
//            logger.info("JDBC-ODBC driver3 ");
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println();
//            logger.info("Failed to reload JDBC-ODBC Driver.");
//        }
//    }

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

    @Transactional
    public Boolean createOrder(OrderDto orderDto) {
        Order order = this.dtoToOrder(orderDto);
        Order savedOrder = null;
        try {
            savedOrder = (Order) this.orderRepo.save(order);
        } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
            System.out.println("from createOrder : Caught ObjectOptimisticLockingFailureException:");
            System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
            System.out.println(order.getId() + " could not be saved due to version mismatch or no matching record.");
            e.printStackTrace();
            logger.info(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            Throwable rootCause = e.getRootCause();
            if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                System.out.println("from create : Caught SQLIntegrityConstraintViolationException:");
                System.out.println(rootCause.getMessage());
                System.out.println(order.getId() + " is not saved");
                e.printStackTrace();
                logger.info(e.getMessage());
                return savedOrder != null;
            } else {
                System.out.println("from create : Caught DataIntegrityViolationException:");
                System.out.println(e.getMessage());
                e.printStackTrace();
                logger.info(e.getMessage());
                return savedOrder != null;
            }
        }
        return savedOrder != null;
    }

    @Override
    public CustomOrderDto createCustomOrder(CustomOrderDto customOrderDto) {
        CustomOrder customOrder = modelMapper.map(customOrderDto, CustomOrder.class);
        CustomOrder savedCustomOrder = customOrderRepo.save(customOrder);
        return modelMapper.map(savedCustomOrder, CustomOrderDto.class);
    }

    @Override
    public DeleteCustOrderDto deleteCustomOrder(CustomOrderDto customOrderDto) {
        CustomOrder customOrder = modelMapper.map(customOrderDto, CustomOrder.class);
        try {
            customOrderRepo.delete(customOrder);
            boolean exists = customOrderRepo.existsById(customOrder.getId());
            boolean isDeleted = !exists ? true : false;
            if (isDeleted) {
                Optional<TransportOrderLines> transportOrderLines = transportOrderLinesRepo.findByOrderIdAndOrderType(customOrder.getId(), OrderType.CUSTOM);
                if (transportOrderLines.isPresent()) {
                    transportOrderLinesRepo.deleteById(transportOrderLines.get().getId());
                    return new DeleteCustOrderDto(transportOrderLines.get(), isDeleted);
                }
            }
            return new DeleteCustOrderDto(null, isDeleted);
        } catch (Exception e) {
            return new DeleteCustOrderDto(null, false);
        }
    }

    @Override
    public List<CustomOrderDto> getAllCustomOrders() {
        return customOrderRepo.findAll().stream().map(co -> modelMapper.map(co, CustomOrderDto.class)).collect(Collectors.toList());
    }

    @Transactional
    public Boolean archiveOrder(OrderDto orderDto) {
        return this.archivedOrdersService.createArchivedOrder(orderDto);
    }

    @Transactional
    public void moveToArchive(List<Integer> ids) {
        logger.info("removing: ");
        for (Integer i : ids) {
            logger.info(i + ",");
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
                    logger.info("removed: " + orderDto.getId());
                }
            }
        }

        if (isMoved) {
            this.deleteOrderData(ids);
        }

    }

    @Transactional
    public void deleteOrderData(List<Integer> ids) {
        try {
            this.orderRepo.deleteMonSubsForIds(ids);
            this.orderRepo.deleteODForIds(ids);
            this.orderRepo.deleteOrdersByIds(ids);
        } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
            System.out.println("from deleteOrderData : Caught ObjectOptimisticLockingFailureException:");
            System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
            System.out.println(ids + " could not be saved due to version mismatch or no matching record.");
            e.printStackTrace();
            logger.info(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            Throwable rootCause = e.getRootCause();
            if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                System.out.println("Caught ids SQLIntegrityConstraintViolationException:");
                System.out.println(rootCause.getMessage());
                System.out.println(ids + " is not saved");
                e.printStackTrace();
                logger.info(e.getMessage());

            } else {
                System.out.println("Caught ids DataIntegrityViolationException:");
                System.out.println(e.getMessage());
                e.printStackTrace();
                logger.info(e.getMessage());

            }
        }
        logger.info("deleted: ");
        for (Integer i : ids) {
            logger.info(i + ",");
        }
    }

    //@Transactional
    public List<OrderDto> updateOrder(OrderDto orderDto, Integer orderId, Boolean flowUpdate) {
        List<OrderDto> lo = this.checkMap();
        Order order = (Order) this.orderRepo.findById(orderId).orElseThrow(() -> {
            return new ResourceNotFoundException("Order", "id", (long) orderId);
        });
        order.setDeliveryDate(orderDto.getDeliveryDate());
        order.setReferenceInfo(orderDto.getReferenceInfo());
        order.setAantal(orderDto.getAantal());
        order.setProduct(orderDto.getProduct());
        order.setCountry(orderDto.getCountry());
        order.setCity(orderDto.getCity());
        order.setOrganization(orderDto.getOrganization());
        order.setCustomerName(orderDto.getCustomerName());
        order.setIsParent(orderDto.getIsParent());
        order.setTekst(orderDto.getTekst());
        order.setBackOrder(orderDto.getBackOrder());
        if (orderDto.getIsExpired() != null) {
            order.setIsExpired(orderDto.getIsExpired());
        }

        if (flowUpdate) {
            if (!order.hasOnlyOneDifference(this.dtoToOrder(orderDto))) {
                return new ArrayList();
            }

            this.updatingFlow(order, orderDto);
        }

        if (order.getCompleted() == null) {
            order.setCompleted("");
        }

        Order updatedOrder = null;
        try {
            updatedOrder = (Order) this.orderRepo.save(order);

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
        } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
            System.out.println("from updateorder : Caught ObjectOptimisticLockingFailureException:");
            System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
            System.out.println(order.getId() + " could not be saved due to version mismatch or no matching record.");
            e.printStackTrace();
            logger.info(e.getMessage());
            return this.orderDtoList;
        } catch (DataIntegrityViolationException e) {
            Throwable rootCause = e.getRootCause();
            if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                System.out.println("Caught SQLIntegrityConstraintViolationException:");
                System.out.println(rootCause.getMessage());
                System.out.println(order.getId() + " is not saved");
                logger.info(e.getMessage());
                e.printStackTrace();
                return this.orderDtoList;
            } else {
                System.out.println("Caught DataIntegrityViolationException:");
                System.out.println(e.getMessage());
                logger.info(e.getMessage());
                e.printStackTrace();
                return this.orderDtoList;
            }
        }
    }

    @Transactional
    public List<OrderDto> updateOrderColors(String orderNumber, String orderDep, String orderStatus, String flowVal) {
        try {
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
        } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
            System.out.println("from updatecolor : Caught ObjectOptimisticLockingFailureException:");
            System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
            System.out.println(" could not be saved due to version mismatch or no matching record.");
            e.printStackTrace();
            logger.info(e.getMessage());

            return this.orderDtoList;
        } catch (DataIntegrityViolationException e) {
            Throwable rootCause = e.getRootCause();
            if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                System.out.println("from updateColor : Caught SQLIntegrityConstraintViolationException:");
                System.out.println(rootCause.getMessage());
                logger.info(e.getMessage());
                e.printStackTrace();
                return this.orderDtoList;
            } else {
                System.out.println("from updateColor : Caught DataIntegrityViolationException:");
                System.out.println(e.getMessage());
                logger.info(e.getMessage());
                e.printStackTrace();
                return this.orderDtoList;
            }
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

    @PostConstruct
    @Transactional
    public void init() {
        // Your method to be called on startup
        System.out.println("App started and init method called");
        getAllOrders();
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

    @Transactional
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
//                Iterator<Map.Entry<String, OrderDto>> var6 = this.ordersMap.entrySet().iterator();
//
//                while (var6.hasNext()) {
//                    Map.Entry<String, OrderDto> entry = (Map.Entry) var6.next();
//                    OrderDto orderDto = (OrderDto) entry.getValue();
//                    orderDto.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
//                    this.orderDtoList.add(orderDto);
//                }
                this.orderDtoList = new ArrayList<>();

                for (Map.Entry<String, OrderDto> entry : this.ordersMap.entrySet()) {
                    OrderDto orderDto = entry.getValue();
                    orderDto.getDepartments().sort(Comparator.comparingInt(OrderDepartment::getDepId));
                    this.orderDtoList.add(orderDto);
                }

                return this.orderDtoList;
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

    public void removingSameArchivedOrders() {
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

        this.deleteOrderData(matchingIds);
    }

    @Async("taskExecutor")
    @Scheduled(fixedRate = 300000)
    public void markExpired() {
        if (!ordersMap.isEmpty()) {
            int retry = 0;
            while (retry < 5 && retry != -1) {
                try {
                    markExpiredInner();
                    retry = -1;
                } catch (Exception e) {
                    logger.info("markExpired sql exc 2");
                    if (e.getMessage() != null && e.getMessage().contains("[Microsoft][ODBC Driver Manager] Invalid string or buffer length")) {
                        retry++;
                    } else {
                        retry++;
                        e.printStackTrace();
                        logger.info(e.getMessage());
                    }
                } finally {
                    //closeConnections();
                }
            }
            if (retry == 5) {
                retry = 0;
                reloadJDBCDriver();
                refreshDataSource();
                while (retry < 5 && retry != -1) {
                    try {
                        markExpiredInner();
                        retry = -1;
                    } catch (Exception e) {
                        logger.info("markExpired sql exc 3");
                        if (e.getMessage() != null && e.getMessage().contains("[Microsoft][ODBC Driver Manager] Invalid string or buffer length")) {
                            retry++;
                            logger.info("3 exp retry is: " + retry);
                        } else {
                            retry++;
                            e.printStackTrace();
                            logger.info("retry num" + retry + ", " + e.getMessage());
                        }
                    } finally {
                        //closeConnections();
                    }
                }
                if (retry == 5) {
                    exitProgram();
                }
                logger.info("fixed");
            } else {
                logger.info("worked");
            }
        }
    }

    private void exitProgram() {
        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }

    public void markExpiredInner() {
        String query = "SELECT \"va-210\".\"cdorder\" AS 'Verkooporder', " +
                "\"va-211\".\"cdprodukt\" AS 'Product' , \"va-211\".\"nrordrgl\" AS 'Regel'" +
                "FROM DATA.PUB.\"va-210\" " +
                "JOIN DATA.PUB.\"va-211\" ON \"va-210\".\"cdorder\" = \"va-211\".\"cdorder\" " +
                "AND \"va-211\".\"cdadmin\" = \"va-210\".\"cdadmin\" " +
                "WHERE (\"va-210\".\"cdstatus\" <> 'Z' And \"va-210\".\"cdstatus\" <> 'B') " +
                "AND \"va-210\".\"cdadmin\" = '01' " +
                "AND \"va-210\".\"cdvestiging\" = 'ree'";

        System.out.println(query);
        try {
            Connection connection = getConnection();
            connection.clearWarnings();

//            try (Statement isolationStmt = connection.createStatement()) {
//                isolationStmt.execute("SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED");
//                logger.info("Isolation level set to READ UNCOMMITTED");
//            }

            Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ResultSet resultSet = statement.executeQuery(query);
            if (activeConnections.isEmpty()) {
                activeConnections.add(connection);
            }
            if (statement != null && resultSet != null) {
                logger.info("----------resultSet----------");
                logger.info(""+resultSet);
                String orderNumber = null;

                List<String> existingOrderNumbers = new ArrayList<>();
                while (resultSet.next()) {
                    if (resultSet.wasNull()) {
                        logger.info("no ordernumer");
                        continue;
                    }
                    orderNumber = resultSet.getString("Verkooporder");
                    String product = resultSet.getString("Product");
                    String regel = resultSet.getString("Regel");
                    existingOrderNumbers.add(orderNumber + "," + regel);
                }
                Set<String> uniqueones = new HashSet<>(existingOrderNumbers);
                logger.info("unique ones: ");
                logger.info("" + uniqueones.size());
                logger.info("total: ");
                logger.info("" + existingOrderNumbers.size());
                LocalDateTime currentDateTime = LocalDateTime.now();
                logger.info("Current DateTime: " + currentDateTime);
                existingOrderNumbers.forEach(logger::info);
                logger.info("" + ordersMap.values().size());
                List<OrderDto> orderList = this.ordersMap.values()
                        .stream()
                        .filter(ord -> {
                            return !existingOrderNumbers.contains(ord.getOrderNumber() + "," + ord.getRegel());
                        })
                        .collect(Collectors.toList());

                logger.info("list1: " + orderList.size());
                List<Integer> filteredOrders = new ArrayList<>();
                if (!this.archivedOrdersService.getAllArchivedOrders().isEmpty()) {
                    List<ArchivedOrdersDto> archivedOrdersList = this.archivedOrdersService.getAllArchivedOrders()
                            .stream()
                            .filter(ord -> {
                                return !existingOrderNumbers.contains(ord.getOrderNumber() + "," + ord.getRegel());
                            })
                            .collect(Collectors.toList());

                    logger.info("list2: " + archivedOrdersList.size());
                    filteredOrders = orderList
                            .stream()
                            .filter(order ->
                                    archivedOrdersList.stream()
                                            .noneMatch(archivedOrder ->
                                                    archivedOrder.getOrderNumber().equals(order.getOrderNumber()) &&
                                                            archivedOrder.getRegel().equals(order.getRegel())
                                            )
                            ).map(OrderDto::getId)
                            .collect(Collectors.toList());

                    logger.info("list3: " + filteredOrders.size());
                } else {
                    filteredOrders = orderList
                            .stream()
                            .map(OrderDto::getId)
                            .collect(Collectors.toList());
                    logger.info("list3.1: " + filteredOrders.size());
                }

                List<OrderDto> matchingObjects = new ArrayList<>();
                if (!filteredOrders.isEmpty()) {
                    List<Integer> finalFilteredOrders = filteredOrders;
                    matchingObjects = ordersMap.values().stream()
                            .filter(obj -> finalFilteredOrders.contains(obj.getId()))
                            .peek(obj -> obj.setIsExpired(true))
                            .collect(Collectors.toList());
                }


                matchingObjects.forEach(obj -> {
                    logger.info("Got Expired: " + obj.getExpired() + "," + obj.getId());
                    try {
                        this.updateOrder(obj, obj.getId(), false);
                    } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
                        System.out.println("from markexpired : Caught ObjectOptimisticLockingFailureException:");
                        System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
                        System.out.println(obj.getId() + " could not be saved due to version mismatch or no matching record.");
                        e.printStackTrace();
                        logger.info(e.getMessage());
                    } catch (DataIntegrityViolationException e) {
                        Throwable rootCause = e.getRootCause();
                        if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                            System.out.println("Caught markexpired SQLIntegrityConstraintViolationException:");
                            System.out.println(rootCause.getMessage());
                            System.out.println(obj.getId() + " is not saved");
                            logger.info(e.getMessage());
                            e.printStackTrace();
                        } else {
                            System.out.println("Caught markexpired DataIntegrityViolationException:");
                            System.out.println(e.getMessage());
                            logger.info(e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
                this.moveToArchive(filteredOrders);
            }
            connection.clearWarnings();
        } catch (SQLException e) {
            logger.info("markExpired sql exc 1");
            logger.info(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    //    @Async("taskExecutor")
//    @Scheduled(fixedRate = 380000)
    public void archiveExpiredOrders() {
        List<OrderDto> orderList = new ArrayList<>(ordersMap.values());

        // Group by orderNumber
        Map<String, List<OrderDto>> groupedByOrderNumber = orderList.stream()
                .collect(Collectors.groupingBy(OrderDto::getOrderNumber));

        // Process each group
        List<Integer> resultIds = new ArrayList<>();
        for (Map.Entry<String, List<OrderDto>> entry : groupedByOrderNumber.entrySet()) {
            List<OrderDto> orders = entry.getValue();
            boolean allExpired = orders.stream()
                    .allMatch(order -> Boolean.TRUE.equals(order.getIsExpired()));

            if (allExpired) {
                resultIds.addAll(orders.stream().map(OrderDto::getId).collect(Collectors.toList()));
            }
        }
        this.moveToArchive(resultIds);
    }

    public List<OrderDto> getCRMOrders() {
        List<OrderDto> orderDtos = null;
        int retry = 0;
        while (retry < 5 && retry != -1) {
            try {
                orderDtos = getCRMOrdersInner();
                retry = -1;
            } catch (Exception e) {
                logger.info("getCRMOrders sql exc 2");
                if (e.getMessage() != null && e.getMessage().contains("[Microsoft][ODBC Driver Manager] Invalid string or buffer length")) {
                    retry++;
                } else {
                    retry++;
                    e.printStackTrace();
                    logger.info(e.getMessage());
                }
            } finally {
                //closeConnections();
            }
        }
        if (retry == 5) {
            retry = 0;
            reloadJDBCDriver();
            refreshDataSource();
            while (retry < 5 && retry != -1) {
                try {
                    orderDtos = getCRMOrdersInner();
                    retry = -1;
                } catch (Exception e) {
                    logger.info("getCRMOrders sql exc 3");
                    if (e.getMessage() != null && e.getMessage().contains("[Microsoft][ODBC Driver Manager] Invalid string or buffer length")) {
                        retry++;
                        logger.info("3 exp retry is: " + retry);
                    } else {
                        retry++;
                        e.printStackTrace();
                        logger.info(e.getMessage());
                    }
                } finally {
                    //closeConnections();
                }
            }
            if (retry == 5) {
                exitProgram();
            }
            logger.info("fixed");
        } else {
            logger.info("worked");
        }
        return orderDtos;
    }

    //@Transactional
    public List<OrderDto> getCRMOrdersInner() {
        //{DataDirect 7.1 OpenEdge Wire Protocol};DSN=AGRPROD
        boolean adjustParentCalled = false;
        boolean createMonCalled = false;
        boolean productNotesCalled = false;
        try {

//            String query = "SELECT \"va-210\".\"cdorder\" AS 'Verkooporder', \"va-210\".\"cdordsrt\" AS 'Ordersoort'," +
//                    " \"va-211\".\"cdborder\" AS 'Backorder', \"va-210\".\"cdgebruiker-init\" AS 'Gebruiker (I)', \"va-210\".\"cddeb\" AS 'Organisatie'," +
//                    " \"ba-001\".\"naamorg\" AS 'Naam', \"ba-012\".\"postcode\" AS 'Postcode', \"ba-012\".\"plaats\" AS 'Plaats'," +
//                    " \"ba-012\".\"cdland\" AS 'Land', \"va-210\".\"datum-lna\" AS 'Leverdatum', \"va-210\".\"opm-30\" AS 'Referentie'," +
//                    " \"va-210\".\"datum-order\" AS 'Datum order', \"va-210\".\"SYS-DATE\" AS 'Datum laatste wijziging'," +
//                    " \"va-210\".\"cdgebruiker\" AS 'Gebruiker (L)', \"va-211\".\"nrordrgl\" AS 'Regel', \"va-211\".\"aantbest\" AS 'Aantal besteld'," +
//                    " \"va-211\".\"aanttelev\" AS 'Aantal geleverd', \"va-211\".\"cdprodukt\" AS 'Product', \"af-801\".\"tekst\" AS 'Omschrijving'," +
//                    " \"va-211\".\"volgorde\" AS 'regelvolgorde', \"bb-043\".\"cdprodgrp\" FROM DATA.PUB.\"af-801\" , DATA.PUB.\"ba-001\" ," +
//                    " DATA.PUB.\"ba-012\" , DATA.PUB.\"bb-043\" , DATA.PUB.\"va-210\" , DATA.PUB.\"va-211\" " +
//                    "WHERE \"ba-001\".\"cdorg\" = \"va-210\".\"cdorg\" AND \"va-211\".\"cdadmin\" = \"va-210\".\"cdadmin\" " +
//                    "AND \"va-211\".\"cdorder\" = \"va-210\".\"cdorder\" " +
//                    "AND \"va-211\".\"cdprodukt\" = \"af-801\".\"cdsleutel1\" AND \"ba-012\".\"id-cdads\" = \"va-211\".\"id-cdads\" " +
//                    "AND \"bb-043\".\"cdprodukt\" = \"va-211\".\"cdprodukt\" AND ((\"af-801\".\"cdtabel\"='bb-062') AND (\"va-210\".\"cdadmin\"='01') " +
//                    "AND (\"va-211\".\"cdadmin\"='01') AND (\"va-210\".\"cdvestiging\"='ree') AND (\"va-210\".\"cdstatus\" <> 'Z' " +
//                    "And \"va-210\".\"cdstatus\" <> 'B') AND (\"bb-043\".\"cdprodcat\"='pro'))";

            String query = "SELECT \"va-210\".\"cdorder\" AS 'Verkooporder', \"va-210\".\"cdordsrt\" AS 'Ordersoort'," +
                    " \"va-211\".\"cdborder\" AS 'Backorder', \"va-210\".\"cdgebruiker-init\" AS 'Gebruiker (I)', \"va-210\".\"cddeb\" AS 'Organisatie'," +
                    " \"ba-001\".\"naamorg\" AS 'Naam', \"ba-012\".\"postcode\" AS 'Postcode', \"ba-012\".\"plaats\" AS 'Plaats'," +
                    " \"ba-012\".\"cdland\" AS 'Land', \"va-210\".\"datum-lna\" AS 'Leverdatum', \"va-210\".\"opm-30\" AS 'Referentie'," +
                    " \"va-210\".\"datum-order\" AS 'Datum order', \"va-210\".\"SYS-DATE\" AS 'Datum laatste wijziging'," +
                    " \"va-210\".\"cdgebruiker\" AS 'Gebruiker (L)', \"va-211\".\"nrordrgl\" AS 'Regel', \"va-211\".\"aantbest\" AS 'Aantal besteld'," +
                    " \"va-211\".\"aanttelev\" AS 'Aantal geleverd', \"va-211\".\"cdprodukt\" AS 'Product', \"af-801\".\"tekst\" AS 'Omschrijving'," +
                    " \"va-211\".\"volgorde\" AS 'regelvolgorde', \"bb-043\".\"cdprodgrp\", \"bb-040\".\"zoeknaam\" FROM DATA.PUB.\"af-801\" , DATA.PUB.\"ba-001\" ," +
                    " DATA.PUB.\"ba-012\" , DATA.PUB.\"bb-043\" , DATA.PUB.\"bb-040\" , DATA.PUB.\"va-210\" , DATA.PUB.\"va-211\" " +
                    "WHERE \"ba-001\".\"cdorg\" = \"va-210\".\"cdorg\" AND \"va-211\".\"cdadmin\" = \"va-210\".\"cdadmin\" " +
                    "AND \"va-211\".\"cdorder\" = \"va-210\".\"cdorder\" " +
                    "AND \"va-211\".\"cdprodukt\" = \"af-801\".\"cdsleutel1\" AND \"va-211\".\"cdprodukt\" = \"bb-040\".\"cdprodukt\" AND \"ba-012\".\"id-cdads\" = \"va-211\".\"id-cdads\" " +
                    "AND \"bb-043\".\"cdprodukt\" = \"va-211\".\"cdprodukt\" AND ((\"af-801\".\"cdtabel\"='bb-062') AND (\"va-210\".\"cdadmin\"='01') " +
                    "AND (\"va-211\".\"cdadmin\"='01') AND (\"va-210\".\"cdvestiging\"='ree') AND (\"va-210\".\"cdstatus\" <> 'Z' " +
                    "And \"va-210\".\"cdstatus\" <> 'B') AND (\"bb-043\".\"cdprodcat\"='pro'))";

            System.out.println("----------query----------");
            System.out.println(query);
            try {
                Connection connection = getConnection();
                connection.clearWarnings();

//                try (Statement isolationStmt = connection.createStatement()) {
//                    isolationStmt.execute("SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED");
//                    logger.info("Isolation level set to READ UNCOMMITTED");
//                }


                Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);


                ResultSet resultSet = statement.executeQuery(query);


                if (activeConnections.isEmpty()) {
                    activeConnections.add(connection);
                }
                //Class.forName(driver);

                if (statement != null) {
                    System.out.println("----------resultSet----------");
                    System.out.println(resultSet);
                    String orderNumber = null;
                    while (resultSet.next()) {
                        if (resultSet.wasNull()) {
                            System.out.println("no ordernumer");
                            continue;
                        }
                        orderNumber = resultSet.getString("Verkooporder");
                        System.out.println(orderNumber);
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
                        String zoeknaam = resultSet.getString("zoeknaam");

                        String deliveryDate2 = "";
                        OrderDto orderDto = new OrderDto();
                        String finalOrderNumber = orderNumber;

                        if (this.archivedOrdersService.getAllArchivedOrders().stream().anyMatch((obj) -> {
                            return obj.getOrderNumber().equals(finalOrderNumber) && obj.getRegel().equals((regel));
                        })) {
                            archivedOrdersService.deleteFromArchive(finalOrderNumber, regel);
                        }

                        if (!this.ordersMap.containsKey(orderNumber + "," + regel)) {
                            logger.info("not contains order: " + orderNumber + ", " + regel);

                            String finalOrderNumber1 = orderNumber;
                            if (this.ordersMap.entrySet().stream().noneMatch((obj) -> {
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
                            orderDto.setZoeknaam(zoeknaam);
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

                            if (this.createOrder(orderDto)) {
                                this.ordersMap.put(orderNumber + "," + regel, orderDto);
                                logger.info("CREATED ORDER : " + orderDto.getOrderNumber());
                            } else {
                                logger.info("Failed to create record in app");
                            }

                        }
                        if (this.ordersMap.containsKey(orderNumber + "," + regel)) {
                            LocalDateTime currentDateTime = LocalDateTime.now();
                            logger.info("Current DateTime: " + currentDateTime);
                            logger.info("contains order: " + orderNumber + ", " + regel);
                            logger.info("back order: " + backOrder);

                            OrderDto existingOrderDto = (OrderDto) this.ordersMap.get(orderNumber + "," + regel);
                            logger.info("existing back order: " + existingOrderDto.getBackOrder());

                            orderDto.setId(existingOrderDto.getId());
                            orderDto.setOrderNumber(orderNumber);
                            orderDto.setOrderType(orderType);
                            orderDto.setBackOrder(backOrder);
                            orderDto.setCdProdGrp(cdProdGrp);
                            orderDto.setZoeknaam(zoeknaam);
                            orderDto.setUser(user);
                            orderDto.setOrganization(organization);
                            orderDto.setCustomerName(customerName);
                            orderDto.setPostCode(postCode);
                            orderDto.setCity(city);
                            orderDto.setIsParent(existingOrderDto.getIsParent());
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
                            orderDto.setRegel(existingOrderDto.getRegel());
                            orderDto.setAantal(aantal);
                            orderDto.setProduct(product);
                            orderDto.setOmsumin(omsumin);

                            Map<String, Boolean> fieldsCheckMap = checkForFeildsChange(existingOrderDto, orderDto);
                            fieldsCheckMap.entrySet().stream()
                                    .forEach(entry -> System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue()));

                            if (fieldsCheckMap.getOrDefault("cdProdGrp", false) || fieldsCheckMap.getOrDefault("orderType", false)) {
                                try {
                                    this.deleteOrderData(Collections.singletonList(existingOrderDto.getId()));
                                    logger.info("DELETING : " + existingOrderDto.getId());
                                } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
                                    System.out.println("from getcrm delete : Caught ObjectOptimisticLockingFailureException:");
                                    System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
                                    System.out.println(existingOrderDto.getId() + " could not be saved due to version mismatch or no matching record.");
                                    e.printStackTrace();
                                    logger.info(e.getMessage());
                                } catch (DataIntegrityViolationException e) {
                                    Throwable rootCause = e.getRootCause();
                                    if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                                        System.out.println("from getcrm delete : Caught SQLIntegrityConstraintViolationException:");
                                        System.out.println(rootCause.getMessage());
                                        System.out.println(existingOrderDto.getId() + " is not saved");
                                        logger.info(e.getMessage());
                                        e.printStackTrace();
                                    } else {
                                        System.out.println("from getcrm delete : Caught DataIntegrityViolationException:");
                                        System.out.println(e.getMessage());
                                        logger.info(e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                                this.settingUpFlow(orderDto);
                                try {
                                    this.createOrder(orderDto);
                                } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
                                    System.out.println("from getcrm createOrder : Caught ObjectOptimisticLockingFailureException:");
                                    System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
                                    System.out.println(orderDto.getId() + " could not be saved due to version mismatch or no matching record.");
                                    logger.info(e.getMessage());
                                    e.printStackTrace();
                                } catch (DataIntegrityViolationException e) {
                                    Throwable rootCause = e.getRootCause();
                                    if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                                        System.out.println("from getcrm create : Caught SQLIntegrityConstraintViolationException:");
                                        System.out.println(rootCause.getMessage());
                                        System.out.println(orderDto.getId() + " is not saved");
                                        logger.info(e.getMessage());
                                        e.printStackTrace();
                                    } else {
                                        System.out.println("from getcrm create : Caught DataIntegrityViolationException:");
                                        System.out.println(e.getMessage());
                                        logger.info(e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                                this.ordersMap.put(orderNumber + "," + regel, orderDto);
                            } else if (fieldsCheckMap.getOrDefault("aantal", false) ||
                                    fieldsCheckMap.getOrDefault("custName", false) ||
                                    fieldsCheckMap.getOrDefault("product", false) ||
                                    fieldsCheckMap.getOrDefault("reference", false) ||
                                    fieldsCheckMap.getOrDefault("organization", false) ||
                                    fieldsCheckMap.getOrDefault("city", false) ||
                                    fieldsCheckMap.getOrDefault("country", false) ||
                                    fieldsCheckMap.getOrDefault("backOrder", false) ||
                                    (!existingOrderDto.getDeliveryDate().equals(deliveryDate2) && !deliveryDate2.isEmpty())) {
                                try {
                                    this.updateOrder(orderDto, orderDto.getId(), false);
                                    System.out.println(existingOrderDto.getDeliveryDate() + ", " + deliveryDate2);
                                    System.out.println("UPDATING : " + orderDto.getId());
                                } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
                                    System.out.println("from getcrm update : Caught ObjectOptimisticLockingFailureException:");
                                    System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
                                    System.out.println(orderDto.getId() + " could not be saved due to version mismatch or no matching record.");
                                    fieldsCheckMap.entrySet().stream().forEach(o -> System.out.println(o.getKey() + ", " + o.getValue()));
                                    logger.info(e.getMessage());
                                    e.printStackTrace();
                                } catch (DataIntegrityViolationException e) {
                                    Throwable rootCause = e.getRootCause();
                                    if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                                        System.out.println("from getcrm update : Caught SQLIntegrityConstraintViolationException:");
                                        System.out.println(rootCause.getMessage());
                                        System.out.println(orderDto.getId() + " is not saved");
                                        logger.info(e.getMessage());
                                        e.printStackTrace();
                                    } else {
                                        System.out.println("from getcrm update : Caught DataIntegrityViolationException:");
                                        System.out.println(e.getMessage());
                                        logger.info(e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
                connection.clearWarnings();
            } catch (SQLException var35) {
                getConnection().clearWarnings();

                logger.info("getCRMOrders sql exc 1");
                var35.printStackTrace();
                if (adjustParentCalled != true && createMonCalled != true && productNotesCalled != true) {

                    //this.adjustParentOrders();

                    adjustParentCalled = true;
                    createMonCalled = true;
                    productNotesCalled = true;
                }
                List<OrderDto> orderList = this.getAllOrders();
                this.orderDtoList = orderList;
                return this.orderDtoList;
                //new ResourceNotFoundException("Order", "CRM", "N/A");
                //return null;
            } catch (Exception var37) {
                getConnection().clearWarnings();

                logger.info("getCRMOrders sql exc 2");
                Exception e = var37;
                e.printStackTrace();
                logger.info(e.getMessage());

                if (adjustParentCalled != true && createMonCalled != true && productNotesCalled != true) {
//                    this.updateProductNotes();
//                    this.createMonSub();
                    //this.adjustParentOrders();

                    adjustParentCalled = true;
                    createMonCalled = true;
                    productNotesCalled = true;
                }
                List<OrderDto> orderList = this.getAllOrders();
                this.orderDtoList = orderList;
                return this.orderDtoList;
                //new ResourceNotFoundException("Order", "CRM", "N/A");
                //return null;
            }

            if (createMonCalled != true && productNotesCalled != true) {
//                this.updateProductNotes();
//                this.createMonSub();

                createMonCalled = true;
                productNotesCalled = true;
            }
            this.ordersMap.clear();
            List<OrderDto> orderList = this.getAllOrders();
            this.orderDtoList = orderList;
            return this.orderDtoList;


        } catch (Exception var39) {
           logger.info("getCRMOrders sql exc 3");
            Exception e = var39;
            e.printStackTrace();
            if (adjustParentCalled != true || (createMonCalled != true && productNotesCalled != true)) {
//                this.updateProductNotes();
//                this.createMonSub();
                //this.adjustParentOrders();
            }
            List<OrderDto> orderList = this.getAllOrders();
            this.orderDtoList = orderList;
            return this.orderDtoList;
            //new ResourceNotFoundException("Order", "CRM", "N/A");
            //return null;
        } finally {
            //closeConnections();
        }
    }

    private Map<String, Boolean> checkForFeildsChange(OrderDto existing, OrderDto orderDto) {

        Map<String, Boolean> checkFieldsMap = new HashMap<>();
        checkFieldsMap.put("cdProdGrp", false);
        checkFieldsMap.put("orderType", false);

        if (!existing.getOrderType().equals(orderDto.getOrderType())) {
            checkFieldsMap.put("orderType", true);
        }
        if (!existing.getOrganization().equals(orderDto.getOrganization())) {
            checkFieldsMap.put("organization", true);
        }
        if (!existing.getCity().equals(orderDto.getCity())) {
            checkFieldsMap.put("city", true);
        }
        if (!existing.getCountry().equals(orderDto.getCountry())) {
            checkFieldsMap.put("country", true);
        }
        if (!existing.getProduct().equals(orderDto.getProduct())) {
            checkFieldsMap.put("product", true);
        }
        if (!existing.getCdProdGrp().equals(orderDto.getCdProdGrp())) {
            checkFieldsMap.put("cdProdGrp", true);
        }
        if (!existing.getCustomerName().equals(orderDto.getCustomerName())) {
            checkFieldsMap.put("custName", true);
        }
        if (!existing.getAantal().equals(orderDto.getAantal())) {
            checkFieldsMap.put("aantal", true);
        }
        if (!existing.getReferenceInfo().equals(orderDto.getReferenceInfo())) {
            checkFieldsMap.put("reference", true);
        }
        if (!existing.getBackOrder().equals(orderDto.getBackOrder())) {
            logger.info("backorder different");
            checkFieldsMap.put("backOrder", true);
        }

        return checkFieldsMap;
    }


    @Transactional
    private void settingUpFlow(OrderDto orderDto) {
        String orderType = orderDto.getOrderType();
        List<OrderDepartment> depList = new ArrayList();
        String wheelOrder = orderDto.getCdProdGrp();
        //String pattern = "(182|183|184|410|440|441|820|821|822|823|824|825|826|850|851)";
        String pattern = ".*(182|183|184|410|440|441|820|821|822|823|824|825|826|850|851).*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(wheelOrder);
        if (matcher.find()) {
            orderDto.setSme("");
            orderDto.setSpu("");
            depList.add(new OrderDepartment(2, "SME", "", this.dtoToOrder(orderDto)));
            depList.add(new OrderDepartment(3, "SPU", "", this.dtoToOrder(orderDto)));
        }

        if (orderDto.getCdProdGrp().contains("400") || orderDto.getCdProdGrp().contains("401") ||
                orderDto.getCdProdGrp().contains("402") || orderDto.getCdProdGrp().contains("403") ||
                orderDto.getCdProdGrp().contains("404") || orderDto.getCdProdGrp().contains("405") ||
                orderDto.getCdProdGrp().contains("406") || orderDto.getCdProdGrp().contains("407") ||
                orderDto.getCdProdGrp().contains("408") || orderDto.getCdProdGrp().contains("409") || orderDto.getCdProdGrp().contains("411") ||
                orderDto.getCdProdGrp().contains("412") || orderDto.getCdProdGrp().contains("413") ||
                orderDto.getCdProdGrp().contains("414") || orderDto.getCdProdGrp().contains("415") ||
                orderDto.getCdProdGrp().contains("416") || orderDto.getCdProdGrp().contains("417") ||
                orderDto.getCdProdGrp().contains("418") || orderDto.getCdProdGrp().contains("419") ||
                orderDto.getCdProdGrp().contains("420") || orderDto.getCdProdGrp().contains("421")) {
            if (orderDto.getSme() == null) {
                orderDto.setSme("");
                depList.add(new OrderDepartment(2, "SME", "", this.dtoToOrder(orderDto)));
            }
        }

        if (orderDto.getCdProdGrp().equals("262") || orderDto.getCdProdGrp().equals("263")) {
            if (orderDto.getSpu() == null) {
                orderDto.setSpu("");
                depList.add(new OrderDepartment(3, "SPU", "", this.dtoToOrder(orderDto)));
            }
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

    @Override
    public void adjustParentOrders() {
        Map<String, List<OrderDto>> groupedOrdersMap = ordersMap.values().stream()
                .collect(Collectors.groupingBy(OrderDto::getOrderNumber));

        for (Map.Entry<String, List<OrderDto>> entry : groupedOrdersMap.entrySet()) {
            List<OrderDto> orderDtos = entry.getValue();

            OrderDto parentOrderDto = orderDtos.stream()
                    .min(Comparator.comparingInt(o -> Integer.parseInt(o.getRegel())))
                    .orElseThrow(() -> new IllegalStateException("No orders found for ordernumber: " + entry.getKey()));

            boolean leastOneIsParentAlready = parentOrderDto.getIsParent() == 1;
            List<OrderDto> ordersToUpdate = new ArrayList<>();

            for (OrderDto orderDto : orderDtos) {
                if (Integer.parseInt(orderDto.getRegel()) != Integer.parseInt(parentOrderDto.getRegel())) {
                    if (orderDto.getIsParent() == 1) {
                        orderDto.setIsParent(0);
                        ordersToUpdate.add(orderDto);
                    }
                }
            }
            if (!leastOneIsParentAlready) {
                parentOrderDto.setIsParent(1);
                ordersToUpdate.add(parentOrderDto);
            }
            for (OrderDto orderDto : ordersToUpdate) {
                System.out.println(orderDto.getId() + ", " + orderDto.getOrderNumber() + ", " + orderDto.getRegel());
            }
            try {
                ordersToUpdate.forEach(order -> {
                    try {
                        updateOrder(order, order.getId(), false);
                    } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
                        System.out.println("from adjustparent insdie: Caught ObjectOptimisticLockingFailureException:");
                        System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
                        System.out.println(order.getId() + " could not be saved due to version mismatch or no matching record.");
                        logger.info(e.getMessage());
                        e.printStackTrace();
                    } catch (DataIntegrityViolationException e) {
                        Throwable rootCause = e.getRootCause();
                        if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                            System.out.println("Caught adjustparent insdie SQLIntegrityConstraintViolationException:");
                            System.out.println(rootCause.getMessage());
                            System.out.println(order.getId() + " is not saved");
                            logger.info(e.getMessage());
                            e.printStackTrace();
                        } else {
                            System.out.println("Caught adjustparent insdie DataIntegrityViolationException:");
                            System.out.println(e.getMessage());
                            logger.info(e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
            } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
                System.out.println("from adjustparent : Caught ObjectOptimisticLockingFailureException:");
                System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
                System.out.println(ordersToUpdate.get(0).getId() + " could not be saved due to version mismatch or no matching record.");
                logger.info(e.getMessage());
                e.printStackTrace();
            } catch (DataIntegrityViolationException e) {
                Throwable rootCause = e.getRootCause();
                if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                    System.out.println("Caught adjustparent SQLIntegrityConstraintViolationException:");
                    System.out.println(rootCause.getMessage());
                    System.out.println(ordersToUpdate.get(0).getId() + " is not saved");
                    logger.info(e.getMessage());
                    e.printStackTrace();
                } else {
                    System.out.println("Caught adjustparent DataIntegrityViolationException:");
                    System.out.println(e.getMessage());
                    logger.info(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public void createMonSubDemo() {

        OrderDto orderDto = ordersMap.getOrDefault("V0500331" + "," + "1", null);

        Order order = orderRepo.findById(orderDto.getId()).get();
        MonSubOrders subOrder = new MonSubOrders();
        subOrder.setOrderNumber(orderDto.getOrderNumber());
        subOrder.setProduct(orderDto.getProduct() + "2");
        subOrder.setRegel("1");
        subOrder.setAantal(order.getAantal());
        subOrder.setOmsumin("some description");
        subOrder.setOrder(order);

        if (order.getMonSubOrders() != null) {
            order.getMonSubOrders().add(subOrder);
        } else {
            List<MonSubOrders> subOrdersList = new ArrayList<>();
            subOrdersList.add(subOrder);
            order.setMonSubOrders(subOrdersList);
        }
        try {
            orderRepo.save(order);
        } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
            System.out.println("from createMonSubDemo : Caught ObjectOptimisticLockingFailureException:");
            System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
            System.out.println(order.getId() + " could not be saved due to version mismatch or no matching record.");
            logger.info(e.getMessage());
            e.printStackTrace();
        } catch (DataIntegrityViolationException e) {
            Throwable rootCause = e.getRootCause();
            if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                System.out.println("from createMonSubDemo : Caught SQLIntegrityConstraintViolationException:");
                System.out.println(rootCause.getMessage());
                System.out.println(order.getId() + " is not saved");
                logger.info(e.getMessage());
                e.printStackTrace();
            } else {
                System.out.println("from createMonSubDemo : Caught DataIntegrityViolationException:");
                System.out.println(e.getMessage());
                logger.info(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void updateProductNotes() {
        int retry = 0;
        while (retry < 5 && retry != -1) {
            try {
                updateProductNotesInner();
                retry = -1;
            } catch (Exception e) {
                logger.info("updateProductNotes sql exc 2");
                if (e.getMessage() != null && e.getMessage().contains("[Microsoft][ODBC Driver Manager] Invalid string or buffer length")) {
                    retry++;
                } else {
                    retry++;
                    e.printStackTrace();
                    logger.info(e.getMessage());
                }
            } finally {
//                closeConnections();
            }
        }
        if (retry == 5) {
            retry = 0;
            reloadJDBCDriver();
            refreshDataSource();
            while (retry < 5 && retry != -1) {
                try {
                    updateProductNotesInner();
                    retry = -1;
                } catch (Exception e) {
                    logger.info("updateProductNotes sql exc 3");
                    if (e.getMessage() != null && e.getMessage().contains("[Microsoft][ODBC Driver Manager] Invalid string or buffer length")) {
                        retry++;
                        logger.info("3 exp retry is: " + retry);
                    } else {
                        retry++;
                        e.printStackTrace();
                        logger.info(e.getMessage());
                    }
                } finally {
                    //closeConnections();
                }
            }
            if (retry == 5) {
                exitProgram();
            }
            logger.info("fixed");
        } else {
            logger.info("worked");
        }
    }

    //@Transactional
    public void updateProductNotesInner() {
        try {
            System.out.println("Orders Map Size: " + ordersMap.size());
            List<String> formattedOrders = new ArrayList<>();
            for (Map.Entry<String, OrderDto> entry : ordersMap.entrySet()) {
                String orderNumbers = entry.getValue().getOrderNumber();
                formattedOrders.add("'" + orderNumbers + "'");
            }

            if (!formattedOrders.isEmpty()) {
                String stringOfOrdersWithCommaAndQuotations = String.join(",", formattedOrders);

                String query = "SELECT \"af-801\".\"cdsleutel1\" AS 'Verkooporder', \"af-801\".\"tekst\" AS 'TekstDescription', \"af-801\".\"cdsleutel2\" AS 'Regel' " +
                        "FROM DATA.PUB.\"af-801\" " +
                        "WHERE \"af-801\".\"cdsoort\" = 'ORR' " +
                        "AND \"af-801\".\"cdsleutel1\" IN (" + stringOfOrdersWithCommaAndQuotations + ")";

                System.out.println("----------query----------");
                System.out.println(query);

                try {
                    Connection connection = getConnection();
                    connection.clearWarnings();

//                    try (Statement isolationStmt = connection.createStatement()) {
//                        isolationStmt.execute("SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED");
//                        logger.info("Isolation level set to READ UNCOMMITTED");
//                    }

                    Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

                    ResultSet resultSet = statement.executeQuery(query);
                    if (activeConnections.isEmpty()) {
                        activeConnections.add(connection);
                    }


                    if (statement != null) {
                        System.out.println("----------resultSet----------");
                        System.out.println(resultSet);
                        if (resultSet != null) {
                            ResultSetMetaData metaData = resultSet.getMetaData();
                            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                System.out.println(metaData.getColumnName(i));
                            }

//                            boolean hasRows = resultSet.isBeforeFirst();
//                            System.out.println("ResultSet has rows: " + hasRows);

//                            if (hasRows) {
                            logger.info("----------resultSet Rows product notes----------");
                            while (resultSet.next()) {

                                String orderNumber = resultSet.getString("Verkooporder");
                                String regel = resultSet.getString("Regel");
                                String text = resultSet.getString("TekstDescription");

                                regel = String.valueOf(Integer.parseInt(regel));

                                OrderDto orderDto = ordersMap.getOrDefault(orderNumber + "," + regel, null);
                                if (orderDto != null) {
                                    if (orderDto.getTekst() != null && !orderDto.getTekst().equals(text)) {
                                        orderDto.setTekst(text);
                                        this.updateOrder(orderDto, orderDto.getId(), false);
                                    }
                                    if (orderDto.getTekst() == null) {
                                        orderDto.setTekst(text);
                                        this.updateOrder(orderDto, orderDto.getId(), false);
                                    }
                                }
                            }
                        }
//                        } else {
//                            System.out.println("ResultSet is empty or null.");
//                        }
                    }
                    connection.clearWarnings();
                } catch (Exception var39) {
                    getConnection().clearWarnings();

                    System.out.println("updateProductNotes sql exc 1");
                    Exception e = var39;
                    logger.info(e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("updateProductNotes sql exc 1");
            logger.info(e.getMessage());
            e.printStackTrace();
        } finally {
            //closeConnections();
        }
    }

    @Override
    public Map<String, OrderDto> updateTextForOrders() {
        Map<String, OrderDto> textMap = null;
        int retry = 0;
        while (retry < 5 && retry != -1) {
            try {
                textMap = updateTextForOrdersInner();
                retry = -1;
            } catch (Exception e) {
                logger.info("updateTextForOrders sql exc 2");
                if (e.getMessage() != null && e.getMessage().contains("[Microsoft][ODBC Driver Manager] Invalid string or buffer length")) {
                    retry++;
                } else {
                    retry++;
                    e.printStackTrace();
                    logger.info(e.getMessage());
                }
            } finally {
                //closeConnections();
            }
        }
        if (retry == 5) {
            retry = 0;
            reloadJDBCDriver();
            refreshDataSource();
            while (retry < 5 && retry != -1) {
                try {
                    textMap = updateTextForOrdersInner();
                    retry = -1;
                } catch (Exception e) {
                    logger.info("updateTextForOrders sql exc 3");
                    if (e.getMessage() != null && e.getMessage().contains("[Microsoft][ODBC Driver Manager] Invalid string or buffer length")) {
                        retry++;
                        logger.info("3 exp retry is: " + retry);
                    } else {
                        retry++;
                        e.printStackTrace();
                        logger.info(e.getMessage());
                    }
                } finally {
                    //closeConnections();
                }
            }
            if (retry == 5) {
                exitProgram();
            }
            logger.info("fixed");
        } else {
            logger.info("worked");
        }
        return textMap;
    }

    public Map<String, OrderDto> updateTextForOrdersInner() {
        try {


            List<String> formattedOrders = new ArrayList<>();
            for (Map.Entry<String, OrderDto> entry : ordersMap.entrySet()) {
                String orderNumbers = entry.getValue().getOrderNumber();
                formattedOrders.add("'" + orderNumbers + "'");
            }

            String stringOfOrdersWithCommaAndQuotations = String.join(",", formattedOrders);

            String query = "SELECT \"af-801\".\"cdsleutel1\" AS 'Verkooporder', \"af-801\".\"tekst\" AS 'TekstDescription', \"af-801\".\"cdsleutel2\" AS 'Regel' " +
                    "FROM DATA.PUB.\"af-801\" " +
                    "WHERE \"af-801\".\"cdsoort\" = 'ORR' " +
                    "AND \"af-801\".\"cdsleutel1\" IN (" + stringOfOrdersWithCommaAndQuotations + ")";

            System.out.println("----------query----------");
            System.out.println(query);
            try {

                Connection connection = getConnection();
                connection.clearWarnings();

//                try (Statement isolationStmt = connection.createStatement()) {
//                    isolationStmt.execute("SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED");
//                    logger.info("Isolation level set to READ UNCOMMITTED");
//                }

                Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

                ResultSet resultSet = statement.executeQuery(query);

                if (activeConnections.isEmpty()) {
                    activeConnections.add(connection);
                }

                if (statement != null) {
                    System.out.println("----------resultSet----------");
                    System.out.println(resultSet);
                    if (resultSet != null) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            System.out.println(metaData.getColumnName(i));
                        }

                        boolean hasRows = resultSet.isBeforeFirst();
                       logger.info("ResultSet has rows: " + hasRows);

                        if (hasRows) {
                            System.out.println("----------resultSet Rows----------");
                            while (resultSet.next()) {

                                String orderNumber = resultSet.getString("Verkooporder");
                                String regel = resultSet.getString("Regel");
                                String text = resultSet.getString("TekstDescription");

                                regel = String.valueOf(Integer.parseInt(regel));

                                OrderDto orderDto = ordersMap.getOrDefault(orderNumber + "," + regel, null);
                                if (orderDto != null) {
                                    if (orderDto.getTekst() != null && !orderDto.getTekst().equals(text)) {
                                        orderDto.setTekst(text);
                                        this.updateOrder(orderDto, orderDto.getId(), false);
                                    }
                                    if (orderDto.getTekst() == null) {
                                        orderDto.setTekst(text);
                                        this.updateOrder(orderDto, orderDto.getId(), false);
                                    }
                                }
                            }
                        }
                    } else {
                        logger.info("ResultSet is empty or null.");
                    }
                }
                connection.clearWarnings();

            } catch (Exception var39) {
                getConnection().clearWarnings();
                System.out.println("updateTextForOrders sql exc 1");
                Exception e = var39;
                logger.info(e.getMessage());
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            System.out.println("updateTextForOrders sql exc 2");
            logger.info(e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            //closeConnections();
        }
        return null;
    }

    @Override
    @Transactional
    public Map<String, OrderDto> createMonSub() {
        Map<String, OrderDto> monSubMap = null;
        int retry = 0;
        while (retry < 5 && retry != -1) {
            try {
                monSubMap = createMonSubInner();
                retry = -1;
            } catch (Exception e) {
                logger.info("createMonSub sql exc 2");
                if (e.getMessage() != null && e.getMessage().contains("[Microsoft][ODBC Driver Manager] Invalid string or buffer length")) {
                    retry++;
                } else {
                    retry++;
                    e.printStackTrace();
                    logger.info(e.getMessage());
                }
            } finally {
                //closeConnections();
            }
        }
        if (retry == 5) {
            retry = 0;
            reloadJDBCDriver();
            refreshDataSource();
            while (retry < 5 && retry != -1) {
                try {
                    monSubMap = createMonSubInner();
                    retry = -1;
                } catch (Exception e) {
                    logger.info("createMonSub sql exc 3");
                    if (e.getMessage() != null && e.getMessage().contains("[Microsoft][ODBC Driver Manager] Invalid string or buffer length")) {
                        retry++;
                        logger.info("3 exp retry is: " + retry);
                    } else {
                        retry++;
                        e.printStackTrace();
                        logger.info(e.getMessage());
                    }
                } finally {
                    //closeConnections();
                }
            }
            if (retry == 5) {
                exitProgram();
            }
            logger.info("fixed");
        } else {
            logger.info("worked");
        }
        return monSubMap;
    }

    public Map<String, OrderDto> createMonSubInner() {
        try {
            List<String> formattedOrders = new ArrayList<>();
            for (Map.Entry<String, OrderDto> entry : ordersMap.entrySet()) {
                if ("MLE".equals(entry.getValue().getOrderType()) ||
                        "MSE".equals(entry.getValue().getOrderType()) ||
                        "MSP".equals(entry.getValue().getOrderType()) ||
                        "MWP".equals(entry.getValue().getOrderType()) ||
                        "MLP".equals(entry.getValue().getOrderType()) ||
                        "MAP".equals(entry.getValue().getOrderType()) ||
                        "MST".equals(entry.getValue().getOrderType()) ||
                        "MLT".equals(entry.getValue().getOrderType()) ||
                        "MSO".equals(entry.getValue().getOrderType()) ||
                        "MWO".equals(entry.getValue().getOrderType()) ||
                        "MLO".equals(entry.getValue().getOrderType()) ||
                        "MAO".equals(entry.getValue().getOrderType())) {

                    String orderNumber = entry.getValue().getOrderNumber();
                    formattedOrders.add("'" + orderNumber + "'");
                }
            }

            if (!formattedOrders.isEmpty()) {
                String stringOfOrdersWithCommaAndQuotations = String.join(",", formattedOrders);

                String query = "SELECT \"va-229\".\"cdorder\" AS 'Verkooporder', \"va-229\".\"nrordrgl\" AS 'Regel', \"va-229\".\"cdprodukt\" AS 'Product', " +
                        "\"va-229\".\"cdadmin\" AS 'Admin', \"af-801\".\"tekst\" AS 'Omschrijving' " +
                        "FROM DATA.PUB.\"va-229\" va " +
                        "LEFT JOIN DATA.PUB.\"af-801\" af ON va.\"cdprodukt\" = af.\"cdsleutel1\" " +
                        "AND af.\"cdtabel\" = 'bb-062' " +
                        "WHERE va.\"cdstatus\" <> 'Z' " +
                        "AND va.\"cdadmin\" = '01' " +
                        "AND va.\"cdorder\" IN (" + stringOfOrdersWithCommaAndQuotations + ")";

                System.out.println("----------query----------");
                System.out.println(query);
                try {
                    Connection connection = getConnection();
                    connection.clearWarnings();

//                    try (Statement isolationStmt = connection.createStatement()) {
//                        isolationStmt.execute("SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED");
//                        logger.info("Isolation level set to READ UNCOMMITTED");
//                    }

                    Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

                    ResultSet resultSet = statement.executeQuery(query);

                    if (activeConnections.isEmpty()) {
                        activeConnections.add(connection);
                    }
                    //Class.forName(driver);
                    if (statement != null && resultSet != null) {
                        System.out.println("----------resultSet----------");
                        System.out.println(resultSet);
                        String orderNumber = null;
                        while (resultSet.next()) {
                            orderNumber = resultSet.getString("Verkooporder");
                            System.out.println(orderNumber);
                            if (resultSet.wasNull()) {
                                System.out.println("no ordernumer");
                                continue;
                            }
                            String regel = resultSet.getString("Regel");
                            String product = resultSet.getString("Product");
                            String description = resultSet.getString("Omschrijving");

                            if (ordersMap.isEmpty()) {
                                try {
                                    this.getAllOrders();
                                } catch (DataIntegrityViolationException e) {
                                    Throwable rootCause = e.getRootCause();
                                    if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                                        System.out.println("from mon getall: Caught SQLIntegrityConstraintViolationException:");
                                        System.out.println(rootCause.getMessage());
                                        logger.info(e.getMessage());
                                        e.printStackTrace();
                                        List<OrderDto> orderList = this.getAllOrders();
                                        this.orderDtoList = orderList;
                                    } else {
                                        System.out.println("from mon getall: Caught DataIntegrityViolationException:");
                                        logger.info(e.getMessage());
                                        System.out.println(e.getMessage());
                                        e.printStackTrace();
                                        List<OrderDto> orderList = this.getAllOrders();
                                        this.orderDtoList = orderList;
                                    }
                                }
                            }

                            OrderDto orderDto = ordersMap.getOrDefault(orderNumber + "," + regel, null);
                            Map<String, Boolean> reminderMap = new HashMap<>();
                            Optional<MonSubOrders> existingSubOrder = Optional.empty();
                            try {
                                existingSubOrder = monSubOrdersRepo.findByOrderNumberAndRegelAndProduct(orderNumber, regel, product);
                            } catch (DataIntegrityViolationException e) {
                                Throwable rootCause = e.getRootCause();
                                if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                                    System.out.println("from mon : Caught SQLIntegrityConstraintViolationException:");
                                    System.out.println(rootCause.getMessage());
                                    logger.info(e.getMessage());
                                    System.out.println(orderNumber + " is not saved");
                                    e.printStackTrace();
                                    List<OrderDto> orderList = this.getAllOrders();
                                    this.orderDtoList = orderList;
                                } else {
                                    System.out.println("from mon : Caught DataIntegrityViolationException:");
                                    logger.info(e.getMessage());
                                    System.out.println(e.getMessage());
                                    e.printStackTrace();
                                    List<OrderDto> orderList = this.getAllOrders();
                                    this.orderDtoList = orderList;
                                }
                            }
                            if (orderDto != null && !product.equals("L") && !existingSubOrder.isPresent() && reminderMap.getOrDefault(orderNumber + "," + product, true)) {
                                try {
                                    Order order = orderRepo.findById(orderDto.getId()).get();
                                    MonSubOrders subOrder = new MonSubOrders();
                                    subOrder.setOrderNumber(orderNumber);
                                    subOrder.setProduct(product);
                                    subOrder.setRegel(regel);
                                    subOrder.setAantal(order.getAantal());
                                    subOrder.setOmsumin(description);
                                    subOrder.setOrder(order);

                                    if (order.getMonSubOrders() != null) {
                                        order.getMonSubOrders().add(subOrder);
                                    } else {
                                        List<MonSubOrders> subOrdersList = new ArrayList<>();
                                        subOrdersList.add(subOrder);
                                        order.setMonSubOrders(subOrdersList);
                                    }
                                    try {
                                        orderRepo.save(order);
                                    } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
                                        System.out.println("from mon : Caught ObjectOptimisticLockingFailureException:");
                                        System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
                                        System.out.println(order.getId() + " could not be saved due to version mismatch or no matching record.");
                                        logger.info(e.getMessage());
                                        e.printStackTrace();
                                    } catch (DataIntegrityViolationException e) {
                                        Throwable rootCause = e.getRootCause();
                                        if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                                            System.out.println("from mon : Caught SQLIntegrityConstraintViolationException:");
                                            System.out.println(rootCause.getMessage());
                                            System.out.println(order.getId() + " is not saved");
                                            logger.info(e.getMessage());
                                            e.printStackTrace();
                                            List<OrderDto> orderList = this.getAllOrders();
                                            this.orderDtoList = orderList;
                                        } else {
                                            System.out.println("from mon : Caught DataIntegrityViolationException:");
                                            System.out.println(e.getMessage());
                                            logger.info(e.getMessage());
                                            e.printStackTrace();
                                            List<OrderDto> orderList = this.getAllOrders();
                                            this.orderDtoList = orderList;
                                        }
                                    }
                                    ordersMap.put(orderNumber + "," + regel, orderToDto(order));
                                    reminderMap.put(orderNumber + "," + product, false);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    connection.clearWarnings();
                } catch (SQLException var35) {
                    getConnection().clearWarnings();
                    System.out.println("createMonSub sql exc 1");
                    logger.info(var35.getMessage());
                    var35.printStackTrace();
                    List<OrderDto> orderList = this.getAllOrders();
                    this.orderDtoList = orderList;
                } catch (Exception var37) {
                    getConnection().clearWarnings();

                    System.out.println("createMonSub sql exc 2");
                    Exception e = var37;
                    logger.info(e.getMessage());
                    e.printStackTrace();
                    List<OrderDto> orderList = this.getAllOrders();
                    this.orderDtoList = orderList;
                }
            }
        } catch (Exception var39) {
            System.out.println("createMonSub sql exc 3");
            Exception e = var39;
            logger.info(e.getMessage());
            e.printStackTrace();
            List<OrderDto> orderList = this.getAllOrders();
            this.orderDtoList = orderList;
        } finally {
            //closeConnections();
        }

        return null;
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
            }
            if (orderDto.getSpu() != null) {
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
                    try {
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
                    } catch (ObjectOptimisticLockingFailureException | StaleStateException e) {
                        System.out.println("from updatetracolors : Caught ObjectOptimisticLockingFailureException:");
                        System.out.println("Optimistic locking failed. Possible concurrent update or stale entity.");
                        System.out.println(ids + " could not be saved due to version mismatch or no matching record.");
                        logger.info(e.getMessage());
                        e.printStackTrace();
                    } catch (DataIntegrityViolationException e) {
                        Throwable rootCause = e.getRootCause();
                        if (rootCause instanceof SQLIntegrityConstraintViolationException) {
                            System.out.println("from updateTraColors : Caught SQLIntegrityConstraintViolationException:");
                            System.out.println(rootCause.getMessage());
                            System.out.println(idList + " is not saved");
                            logger.info(e.getMessage());
                            e.printStackTrace();
                        } else {
                            System.out.println("from updateTraColors : Caught DataIntegrityViolationException:");
                            System.out.println(e.getMessage());
                            logger.info(e.getMessage());
                            e.printStackTrace();
                        }
                    }
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

    public void generateExcelFile(OutputStream outputStream) {
        List<OrderDto> orders = getAllOrders();

        // Create a workbook and sheet
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Orders");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Order Number", "Product Number", "Regel", "Creation", "Organization", "Delivery"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(getHeaderStyle(workbook));
        }

        orders.sort(Comparator.comparing(OrderDto::getOrderNumber)
                .thenComparing(order -> Integer.parseInt(order.getRegel())));


        // Add data rows
        int rowIdx = 1;
        for (OrderDto order : orders) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(order.getOrderNumber());
            row.createCell(1).setCellValue(order.getProduct());
            row.createCell(2).setCellValue(order.getRegel());
            row.createCell(3).setCellValue(order.getCreationDate());
            row.createCell(4).setCellValue(order.getOrganization());
            row.createCell(5).setCellValue(order.getDeliveryDate());
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to output stream
        try {
            workbook.write(outputStream);
            workbook.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CellStyle getHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    public Order dtoToOrder(OrderDto orderDto) {
        Order order = (Order) this.modelMapper.map(orderDto, Order.class);
        return order;
    }

    @Transactional
    public OrderDto orderToDto(Order order) {
        OrderDto orderDto = (OrderDto) this.modelMapper.map(order, OrderDto.class);
        return orderDto;
    }
}