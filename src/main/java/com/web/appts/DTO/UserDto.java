
package com.web.appts.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.web.appts.entities.Department;
import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

public class UserDto {
    private long id;
    private @NotEmpty @Size(
            min = 3,
            message = "Username must be at least 3 characters"
    ) String name;
    private String email;
    private @NotEmpty @Size(
            min = 4,
            max = 16,
            message = "Password should be between 4-16 characters"
    ) String password;
    private String about;
    private int depId;
    private Boolean active;
    private Set<Department> departmentsSet = new HashSet();

    public UserDto() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore
    public String getPassword() {
        return this.password;
    }

    @JsonProperty
    public void setPassword(String password) {
        this.password = password;
    }

    public String getAbout() {
        return this.about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public Boolean getActive() {
        return this.active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public int getDepId() {
        return this.depId;
    }

    public void setDepId(int depId) {
        this.depId = depId;
    }

    public Set<Department> getDepartmentsSet() {
        return this.departmentsSet;
    }

    public void setDepartmentsSet(Set<Department> departmentsSet) {
        this.departmentsSet = departmentsSet;
    }
}
