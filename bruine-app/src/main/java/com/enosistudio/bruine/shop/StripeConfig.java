package com.enosistudio.bruine.shop;

import com.stripe.Stripe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Initialise la clé secrète Stripe (mode Sandbox / test) au démarrage.
 * La clé est lue depuis {@code stripe.secret-key} (application.properties / variable d'env).
 */
@Configuration
public class StripeConfig {

    public StripeConfig(@Value("${stripe.secret-key}") String secretKey) {
        Stripe.apiKey = secretKey;
    }
}
