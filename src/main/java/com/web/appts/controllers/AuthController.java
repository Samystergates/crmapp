package com.web.appts.controllers;

import com.web.appts.DTO.JwtAuthRequest;
import com.web.appts.DTO.JwtAuthResponse;
import com.web.appts.DTO.OrderDto;
import com.web.appts.DTO.UserDto;
import com.web.appts.configurations.JwtTokenHelper;
import com.web.appts.entities.Department;
import com.web.appts.entities.User;
import com.web.appts.exceptions.ApiException;
import com.web.appts.repositories.UserRepo;
import com.web.appts.services.OrderService;
import com.web.appts.services.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping({"/api/v1/auth/"})
public class AuthController {
    @Autowired
    private JwtTokenHelper jwtTokenHelper;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ModelMapper mapper;

    @PostMapping({"/login"})
    public ResponseEntity<JwtAuthResponse> createToken(@RequestBody JwtAuthRequest request) throws Exception {
        authenticate(request.getUsername(), request.getPassword());
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(request.getUsername());
        String token = this.jwtTokenHelper.generateToken(userDetails);

        JwtAuthResponse response = new JwtAuthResponse();
        response.setToken(token);
        response.setUser((UserDto)this.mapper.map(userDetails, UserDto.class));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private void authenticate(String username, String password) throws Exception {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);

        try {
            this.authenticationManager.authenticate(authenticationToken);
        } catch (BadCredentialsException e) {
            System.out.println("Invalid Details !!");
            throw new ApiException("Invalid Username or Password");
        }
    }

    @PostMapping({"/register"})
    public ResponseEntity<UserDto> registerUser(@Valid @RequestBody UserDto userDto) {
        Department department = null;
        if (userDto.getDepId() < 1) {
            int retrievedValue = -1;

            Iterator<Department> iterator = userDto.getDepartmentsSet().iterator();
            if (iterator.hasNext()) {
                retrievedValue = iterator.next().getId();
                department = this.userService.getDepartmentById(retrievedValue);
            }
        } else {
            department = this.userService.getDepartmentById(userDto.getDepId());
        }

        Set<Department> dep = new HashSet<>();
        dep.add(department);
        userDto.setDepartmentsSet(dep);

        UserDto registeredUser = this.userService.registerNewUser(userDto);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    @GetMapping({"/current-user/"})
    public ResponseEntity<UserDto> getUser(Principal principal) {
        User user = this.userRepo.findByEmail(principal.getName()).get();
        return new ResponseEntity<>(this.mapper.map(user, UserDto.class), HttpStatus.OK);
    }

    @GetMapping({"/removing-same-orders"})
    public ResponseEntity<Integer> removeArchiveAndMainOrders() {
        this.orderService.removingSameArchivedOrders();
        return ResponseEntity.ok(1);
    }

    @GetMapping({"/mon-sub-orders"})
    public ResponseEntity<Integer> getMonSub() {
        this.orderService.createMonSub();
        return ResponseEntity.ok(1);
    }

    @GetMapping({"/verify-crm-orders"})
    public ResponseEntity<Integer> getCRMVerification() {
        this.orderService.updateTextForOrders();
        return ResponseEntity.ok(1);
    }

    @GetMapping({"/adjust-parent-orders"})
    public ResponseEntity<Integer> adjustingParents() {
        this.orderService.adjustParentOrders();
        return ResponseEntity.ok(1);
    }

    @GetMapping({"/mark-orders-expired"})
    public ResponseEntity<Integer> markOrdersExpired() {
        this.orderService.markExpired();
        return ResponseEntity.ok(1);
    }

}
