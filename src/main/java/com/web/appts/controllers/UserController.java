
package com.web.appts.controllers;

import com.web.appts.DTO.ApiResponse;
import com.web.appts.DTO.UserDto;
import com.web.appts.configurations.CustomUserDetailService;
import com.web.appts.entities.Department;
import com.web.appts.services.UserService;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
@RequestMapping({"/api/user"})
@CrossOrigin
public class UserController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserService userService;
    @Autowired
    private CustomUserDetailService userDetailsService;

    public UserController() {
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({"/admin/usermanagement"})
    public ResponseEntity<UserDto> createUser(@RequestBody @Valid UserDto userDto) {
        UserDto createUser = this.userService.createUser(userDto);
        return new ResponseEntity(createUser, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping({"/admin/usermanagement/{userId}"})
    public ResponseEntity<UserDto> updateUser(@RequestBody @Valid UserDto userDto, @PathVariable("userId") Long userId) {
        UserDto updateUser = this.userService.updateUser(userDto, userId);
        return ResponseEntity.ok(updateUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping({"/admin/usermanagement/{userId}"})
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable("userId") Long userId) {
        this.userService.deleteUser(userId);
        return new ResponseEntity(new ApiResponse("User Deleted Successfully", true), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping({"/admin/usermanagement/get/{userId}"})
    public ResponseEntity<UserDto> getUser(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(this.userService.getUserById(userId));
    }

    @GetMapping({"/login"})
    public void userLogin() {
    }

    @GetMapping({"/login/l"})
    public void userLoginL() {
        try {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername("admin");
            System.out.println(userDetails.getPassword());
            if (userDetails.getPassword().equals("$2a$10$1P6Ij/fdzehMOl6O90CBEOgg6whPdClRL2QlIzsH0jJz4aWLVtJjG")) {
                Authentication authentication = this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("admin", "admin"));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (AuthenticationException var3) {
        }

    }

    @PostMapping({"/logout"})
    public void logout(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        System.out.println("being called 1");
        SecurityContextHolder.clearContext();
        System.out.println(SecurityContextHolder.getContext());
        System.out.println("being called");
    }

    @GetMapping({"/departments"})
    public ResponseEntity<List<Department>> getDepartments() {
        List<Department> deps = this.userService.getDepartments();
        return ResponseEntity.ok(deps);
    }
}
