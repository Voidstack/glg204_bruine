package com.enosistudio.bruine.admin.mfa;

public enum EAdminRole {
    ADMIN, PRE_MFA;

    public String authority() {
        return "ROLE_" + this.name();
    }
}
