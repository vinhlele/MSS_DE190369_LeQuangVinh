package com.mss301.brand.repository;

import com.mss301.brand.entity.WatchBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// [TODO-5.1: BrandRepository]
@Repository
public interface BrandRepository extends JpaRepository<WatchBrand, Integer> {
}
