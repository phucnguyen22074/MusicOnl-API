package com.example.demo.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.Users;

@Repository
public interface UserRepository extends CrudRepository<Users, Integer>{
	
	@Query("FROM Users Where email = :email")
	public Users findbyEmail(@Param("email") String email);

}
