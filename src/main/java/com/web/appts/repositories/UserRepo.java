
package com.web.appts.repositories;

import com.web.appts.entities.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String paramString);

  Optional<User> findByName(String paramString);
}
