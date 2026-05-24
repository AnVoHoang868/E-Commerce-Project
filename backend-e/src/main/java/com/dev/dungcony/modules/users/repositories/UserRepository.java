package com.dev.dungcony.modules.users.repositories;

import com.dev.dungcony.modules.users.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {


    Optional<User> findByLastName(String lname);

    Optional<User> findByFirstName(String fname);

    Optional<User> findByAccountId(Integer accountId);

    @Query(value = """
            SELECT * FROM tbl_users u
            WHERE unaccent(LOWER(CONCAT(COALESCE(u.f_name,''), ' ', COALESCE(u.l_name,''))))
                  LIKE unaccent(LOWER(CONCAT('%', :name, '%')))
               OR unaccent(LOWER(CONCAT(COALESCE(u.l_name,''), ' ', COALESCE(u.f_name,''))))
                  LIKE unaccent(LOWER(CONCAT('%', :name, '%')))
            """, nativeQuery = true)
    List<User> findByName(@Param("name") String name);
}
