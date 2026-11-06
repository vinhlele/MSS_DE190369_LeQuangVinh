package com.mss301.watch.repository;

import com.mss301.watch.entity.WatchCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// [TODO-6.16: WatchCategoryRepository]
@Repository
public interface WatchCategoryRepository extends JpaRepository<WatchCategory, Integer> {
}
