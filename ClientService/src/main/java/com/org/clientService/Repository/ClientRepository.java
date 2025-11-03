package com.org.clientService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.clientService.Entity.Client;

@Repository
public interface ClientRepository  extends JpaRepository<Client, String>{

}
