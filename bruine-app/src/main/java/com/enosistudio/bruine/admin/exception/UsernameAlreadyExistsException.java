package com.enosistudio.bruine.admin.exception;

import com.enosistudio.bruine.common.BusinessRuleException;

public class UsernameAlreadyExistsException extends BusinessRuleException {

    public UsernameAlreadyExistsException(String username) {
        super("Le nom d'utilisateur « " + username + " » existe déjà.");
    }
}
