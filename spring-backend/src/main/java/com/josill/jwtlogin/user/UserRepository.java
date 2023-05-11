package com.josill.jwtlogin.user;

import java.util.Optional;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsUserByEmail(String email);
    Optional<User> findById(Long id);
    @Transactional // if the update fails the database will be restored to the last state
    @Modifying // method modifies the database, required for all update and delete operations
    @Query("UPDATE User u " + // customUser because in the database the entity name is customUser (check user class)
            "SET u.isEnabled = TRUE WHERE u.email = ?1")
    int enableUser(String email);

}
