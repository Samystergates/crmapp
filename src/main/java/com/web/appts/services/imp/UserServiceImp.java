
package com.web.appts.services.imp;

import com.web.appts.DTO.UserDto;
import com.web.appts.entities.Department;
import com.web.appts.entities.Role;
import com.web.appts.entities.User;
import com.web.appts.exceptions.ResourceNotFoundException;
import com.web.appts.repositories.DepartmentRepo;
import com.web.appts.repositories.RoleRepo;
import com.web.appts.repositories.UserRepo;
import com.web.appts.services.UserService;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImp implements UserService {
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private DepartmentRepo departmentRepo;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleRepo roleRepo;

    public UserServiceImp() {
    }

    public UserDto createUser(UserDto userDto) {
        User user = this.dtoToUser(userDto);
        user.setPassword(this.passwordEncoder.encode(userDto.getPassword()));
        User savedUser = (User)this.userRepo.save(user);
        return this.userToDto(savedUser);
    }

    public UserDto updateUser(UserDto userDto, Long userId) {
        User user = (User)this.userRepo.findById(userId).orElseThrow(() -> {
            return new ResourceNotFoundException("User", "id", (long)userId.intValue());
        });
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPassword(this.passwordEncoder.encode(userDto.getPassword()));
        user.setAbout(userDto.getAbout());
        User updatedUser = (User)this.userRepo.save(user);
        UserDto updatedUserDto = this.userToDto(updatedUser);
        return updatedUserDto;
    }

    public UserDto getUserById(Long userId) {
        User user = (User)this.userRepo.findById(userId).orElseThrow(() -> {
            return new ResourceNotFoundException("User", "id", (long)userId.intValue());
        });
        return this.userToDto(user);
    }

    public List<UserDto> getAllUsers() {
        List<User> users = this.userRepo.findAll();
        List<UserDto> userDtos = (List)users.stream().map((user) -> {
            return this.userToDto(user);
        }).collect(Collectors.toList());
        return userDtos;
    }

    public void deleteUser(Long userId) {
        User user = (User)this.userRepo.findById(userId).orElseThrow(() -> {
            return new ResourceNotFoundException("User", "id", (long)userId.intValue());
        });
        this.userRepo.delete(user);
    }

    public User dtoToUser(UserDto userDto) {
        User user = (User)this.modelMapper.map(userDto, User.class);
        return user;
    }

    public UserDto userToDto(User user) {
        UserDto userDto = (UserDto)this.modelMapper.map(user, UserDto.class);
        return userDto;
    }

    public UserDto registerNewUser(UserDto userDto) {
        User user = (User)this.modelMapper.map(userDto, User.class);
        System.out.println(user.getDepId());
        System.out.println("PRRINTTIINNG");
        System.out.println(user.getDepartmentsSet());
        user.setPassword(this.passwordEncoder.encode(user.getPassword()));
        Role role;
        if (userDto.getDepartmentsSet().stream().anyMatch((e) -> {
            return e.getDepName().equals("ADMIN");
        })) {
            role = (Role)this.roleRepo.findById(1).get();
        } else {
            role = (Role)this.roleRepo.findById(2).get();
        }

        user.getRoles().add(role);
        User newUser = (User)this.userRepo.save(user);
        return (UserDto)this.modelMapper.map(newUser, UserDto.class);
    }

    public List<Department> getDepartments() {
        List<Department> deps = this.departmentRepo.findAll();
        return deps;
    }

    public Department getDepartmentById(Integer depId) {
        Department dep = (Department)this.departmentRepo.findById(depId).orElseThrow(() -> {
            return new ResourceNotFoundException("Department", "id", (long)depId);
        });
        return dep;
    }
}
