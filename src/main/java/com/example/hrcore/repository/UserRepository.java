package com.example.hrcore.repository;

import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    
    Optional<User> findByEmail(String email);
    
    List<User> findByManagerId(UUID managerId);
    
    @Query("SELECT u FROM User u WHERE u.manager.id = :managerId")
    List<User> findDirectReports(@Param("managerId") UUID managerId);
    
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") UserRole role);
    
    boolean existsByEmail(String email);
}

