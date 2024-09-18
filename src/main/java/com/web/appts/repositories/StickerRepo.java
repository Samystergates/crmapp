
package com.web.appts.repositories;

import com.web.appts.entities.StickerLabel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StickerRepo extends JpaRepository<StickerLabel, Long> {
    StickerLabel findByProduct(String productNum);
}
