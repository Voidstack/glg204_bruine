package com.enosistudio.bruine.shop;

import com.enosistudio.bruine.shop.service.ShopService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Séparé de {@link ShopController} parce qu'une panne Stripe ne se traite pas pareil des deux côtés :
 * ici Stripe attend un code HTTP et rejoue tant qu'il n'est pas 2xx, là-bas le joueur attend une page.
 */
@RestController // Parce que serveur à serveur, pas de redirection vers une page HTML
@RequestMapping("/shop/webhook")
public class ShopWebhookController {

    private final ShopService shopService;

    public ShopWebhookController(ShopService shopService) {
        this.shopService = shopService;
    }

    /**
     * Notification serveur à serveur de Stripe : crédite le joueur même s'il ferme l'onglet avant
     * d'être revenu sur {@code /shop/success}. Toute réponse autre que 2xx fait réessayer Stripe.
     */
    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody String payload,
                                        @RequestHeader("Stripe-Signature") String signature) throws StripeException {
        Optional<String> sessionId = shopService.paidCheckoutSessionId(payload, signature);
        if (sessionId.isPresent()) {
            shopService.fulfillCheckout(sessionId.get());
        }
        return ResponseEntity.ok().build();
    }

    // 400 et non 500 : l'événement n'est pas de Stripe, le rejouer ne changerait rien
    @ExceptionHandler(SignatureVerificationException.class)
    public ResponseEntity<Void> onInvalidSignature() {
        return ResponseEntity.badRequest().build();
    }
}
