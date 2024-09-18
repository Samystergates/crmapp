
package com.web.appts.services;

import com.web.appts.DTO.UserDto;
import com.web.appts.entities.Department;
import java.util.List;

public interface UserService {
  UserDto createUser(UserDto paramUserDto);

  UserDto updateUser(UserDto paramUserDto, Long paramInteger);

  UserDto getUserById(Long paramInteger);

  List<UserDto> getAllUsers();

  UserDto registerNewUser(UserDto paramUserDto);

  void deleteUser(Long paramInteger);

  List<Department> getDepartments();

  Department getDepartmentById(Integer paramInteger);
}
