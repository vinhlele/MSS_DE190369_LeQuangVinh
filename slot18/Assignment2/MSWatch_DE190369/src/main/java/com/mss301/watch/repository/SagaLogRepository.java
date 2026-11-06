package com.mss301.watch.repository;

import com.mss301.watch.entity.SagaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// [TODO-6.2: SagaLogRepository]
@Repository
public interface SagaLogRepository extends JpaRepository<SagaLog, Long> {
    List<SagaLog> findBySagaIdOrderByIdAsc(String sagaId);
}
