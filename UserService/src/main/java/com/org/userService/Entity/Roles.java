package com.org.userService.Entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "ROLES")
public class Roles {

	@Id
    private String roleId;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;
    
    @PrePersist
    public void generateId() {
        if (this.roleId == null) {
            String uniquePart = UUID.randomUUID().toString().substring(0, 8);
            this.roleId = "ROL" + uniquePart;
        }
    }
}
