package com.mss301.watch.repository;

import com.mss301.watch.entity.Watch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// [TODO-6.16: WatchRepository]
@Repository
public interface WatchRepository extends JpaRepository<Watch, Integer> {
    List<Watch> findAllByOrderByWatchIdDesc();
}
