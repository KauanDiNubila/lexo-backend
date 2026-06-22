package app.lexo.repository;

import app.lexo.domain.RateHit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface RateHitRepository extends JpaRepository<RateHit, String> {

    @Modifying
    @Transactional
    @Query("delete from RateHit r where r.key = :key and r.createdAt < :before")
    void deleteExpired(@Param("key") String key, @Param("before") Instant before);

    @Query("select count(r) from RateHit r where r.key = :key and r.createdAt >= :since")
    long countSince(@Param("key") String key, @Param("since") Instant since);
}
