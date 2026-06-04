package com.hotelos.reception.repository;

import com.hotelos.shared.entities.Guest;
import com.hotelos.shared.enums.GuestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {

    Optional<Guest> findByEmail(String email);

    List<Guest> findByStatus(GuestStatus status);

    boolean existsByEmail(String email);

    Optional<Guest> findByIdAndStatus(Long id, GuestStatus status);

    long countByStatus(GuestStatus status);
}
