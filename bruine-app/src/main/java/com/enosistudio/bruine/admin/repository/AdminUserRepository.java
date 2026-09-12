package com.enosistudio.bruine.admin.repository;

import com.enosistudio.bruine.admin.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, String> {

}
