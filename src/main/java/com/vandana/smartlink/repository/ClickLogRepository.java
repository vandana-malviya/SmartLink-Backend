package com.vandana.smartlink.repository;

import com.vandana.smartlink.model.ClickLog;
import com.vandana.smartlink.model.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickLogRepository extends JpaRepository<ClickLog, Long> {

    long countByShortLinkAndDeviceType(ShortLink shortLink, String deviceType);

    List<ClickLog> findByShortLink(ShortLink shortLink);

    long countByShortLink(ShortLink shortLink);
}
