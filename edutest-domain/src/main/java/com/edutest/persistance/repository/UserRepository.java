package com.edutest.persistance.repository;

import com.edutest.api.model.UserProfile;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<UserProfile, Integer> {

    Optional<UserProfile> findByUsername(String username);


}
