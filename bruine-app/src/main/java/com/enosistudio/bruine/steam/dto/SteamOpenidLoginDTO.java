package com.enosistudio.bruine.steam.dto;

import jakarta.validation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import java.util.Set;

@Setter
@Getter
public class SteamOpenidLoginDTO {
    @URL
    @NotBlank
    private String ns;

    @URL
    @NotBlank
    private String opEndpoint;

    @URL
    @NotBlank
    private String claimedId;

    @URL
    @NotBlank
    private String identity;

    @URL
    @NotBlank
    private String returnTo;

    @NotBlank
    private String responseNonce;

    @NotBlank
    private String assocHandle;

    @NotBlank
    @Pattern(regexp = "^\\w+(?:,\\w+)*$")
    private String signed;

    @NotBlank
    private String sig;

    public SteamOpenidLoginDTO() {
    }

    public SteamOpenidLoginDTO(String ns, String opEndpoint, String claimedId, String identity, String returnTo, String responseNonce, String assocHandle, String signed, String sig) {
        this.ns = ns;
        this.opEndpoint = opEndpoint;
        this.claimedId = claimedId;
        this.identity = identity;
        this.returnTo = returnTo;
        this.responseNonce = responseNonce;
        this.assocHandle = assocHandle;
        this.signed = signed;
        this.sig = sig;

        this.validate();
    }

    private void validate() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<SteamOpenidLoginDTO>> violations = validator.validate(this);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
    }
}