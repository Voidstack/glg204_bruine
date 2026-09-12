package com.enosistudio.bruine.admin.service;

import com.enosistudio.bruine.admin.domain.AdminUser;
import com.enosistudio.bruine.admin.exception.UsernameAlreadyExistsException;
import com.enosistudio.bruine.admin.repository.AdminUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminUserService implements UserDetailsService {

    private final AdminUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(AdminUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<AdminUser> optAdminUser = repository.findById(username);
        if (optAdminUser.isPresent()) {
            return User.builder()
                    .username(username)
                    .password(optAdminUser.get().getUserPassword())
                    .roles("ADMIN")
                    .build();
        } else {
            throw new UsernameNotFoundException(username);
        }
    }

    @Transactional
    public AdminUser createUser(String username, String password) throws UsernameAlreadyExistsException {
        if (repository.existsById(username)) {
            throw new UsernameAlreadyExistsException();
        }
        String encodedPassword = passwordEncoder.encode(password);
        return repository.save(new AdminUser(username, encodedPassword));
    }

    @Transactional(readOnly = true)
    public List<AdminUser> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void deleteUser(String username) {
        repository.deleteById(username);
    }

    @Transactional
    public void updatePassword(String username, String newPassword) {
        AdminUser user = repository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        user.setUserPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean isMfaEnabled(String username) {
        return repository.findById(username).map(AdminUser::isMfaEnabled).orElse(false);
    }

    @Transactional(readOnly = true)
    public String getMfaSecret(String username) {
        return repository.findById(username).map(AdminUser::getMfaSecret).orElse(null);
    }

    @Transactional
    public void enableMfa(String username, String secret) {
        AdminUser user = repository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        user.setMfaSecret(secret);
        user.setMfaEnabled(true);
        repository.save(user);
    }

    @Transactional
    public void disableMfa(String username) {
        AdminUser user = repository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        user.setMfaSecret(null);
        user.setMfaEnabled(false);
        repository.save(user);
    }
}
