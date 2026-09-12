package com.enosistudio.bruine.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Entity
@Table(name="admin_user")
public class AdminUser {
    @Id
    @Size(max = 20)
    @NotBlank
    @Column(name = "username")
    private String username;

    @Setter
    @Size(max = 500)
    @Column(name = "userpassword")
    @NotBlank
    private String userPassword;

    /** Secret TOTP (base32) partagé avec l'appli d'authentification. Null si MFA désactivée. */
    @Setter
    @Size(max = 64)
    @Column(name = "mfa_secret")
    private String mfaSecret;

    /** MFA activée pour ce compte. */
    @Setter
    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    public AdminUser(@Size(max = 20) @NotBlank String username, @NotBlank String password) {
        this.username = username;
        this.userPassword = password;
    }

}
