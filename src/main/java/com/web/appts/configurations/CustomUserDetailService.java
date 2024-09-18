package com.web.appts.configurations;

import com.web.appts.entities.User;
import com.web.appts.exceptions.ResourceNotFoundException;
import com.web.appts.repositories.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailService implements UserDetailsService {
    @Autowired
    private UserRepo userRepo;

    public CustomUserDetailService() {
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = (User)this.userRepo.findByName(username).orElseThrow(() -> {
            return new ResourceNotFoundException("User", "name", username);
        });
        return user;
    }
}
