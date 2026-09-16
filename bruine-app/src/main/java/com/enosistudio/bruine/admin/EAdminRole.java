package com.enosistudio.bruine.admin;

public enum EAdminRole {
    ADMIN, PRE_MFA;

    public String authority() {
        return "ROLE_" + this.name();
    }
}
