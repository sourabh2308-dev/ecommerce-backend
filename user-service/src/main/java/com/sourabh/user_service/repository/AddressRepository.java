package com.sourabh.user_service.repository;

import com.sourabh.user_service.entity.Address;
import com.sourabh.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);

    Optional<Address> findByUuidAndUser(String uuid, User user);

    Optional<Address> findByUserAndIsDefaultTrue(User user);

    int countByUser(User user);
}
