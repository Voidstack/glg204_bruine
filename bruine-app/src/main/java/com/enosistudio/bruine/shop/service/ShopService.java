package com.enosistudio.bruine.shop.service;

import com.enosistudio.bruine.shop.model.ShopPack;
import com.enosistudio.bruine.shop.model.ShopPurchase;
import com.enosistudio.bruine.shop.repository.ShopPackRepository;
import com.enosistudio.bruine.shop.repository.ShopPurchaseRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.service.SteamUserService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ShopService {

    private final ShopPackRepository packRepository;
    private final ShopPurchaseRepository purchaseRepository;
    private final SteamUserService steamUserService;

    public ShopService(ShopPackRepository packRepository,
                       ShopPurchaseRepository purchaseRepository,
                       SteamUserService steamUserService) {
        this.packRepository = packRepository;
        this.purchaseRepository = purchaseRepository;
        this.steamUserService = steamUserService;
    }

    // Packs

    /**
     * Packs triés pour l'affichage (ordre admin puis id).
     */
    @Transactional(readOnly = true)
    public List<ShopPack> findAllOrdered() {
        return packRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional(readOnly = true)
    public Optional<ShopPack> findById(Long id) {
        return packRepository.findById(id);
    }

    /**
     * Sauvegarde un pack. Un seul pack peut être « Populaire » à la fois :
     * si celui-ci l'est, on retire le drapeau des autres.
     */
    @Transactional
    public ShopPack save(ShopPack pack) {
        ShopPack saved = packRepository.save(pack);
        if (saved.isPopular()) {
            packRepository.clearPopularExcept(saved.getId());
        }
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        packRepository.deleteById(id);
    }

    // Paiement Stripe (Checkout, mode Sandbox)

    /**
     * Crée une session Stripe Checkout pour l'achat d'un pack et renvoie l'URL de paiement
     * (page hébergée par Stripe). Le pack et l'utilisateur sont passés en metadata pour être
     * retrouvés au moment du crédit des points.
     * <p>
     * Aucune transaction ici : la méthode ne touche pas la base, et ouvrir une transaction
     * autour d'un appel réseau immobiliserait une connexion JDBC le temps de la réponse Stripe.
     *
     * @param baseUrl base publique de l'application (ex. http://localhost:8080) pour les URLs de retour
     */
    public String createCheckoutSession(SteamUser user, ShopPack pack, String baseUrl) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(baseUrl + "/shop/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(baseUrl + "/shop/cancel")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount((long) pack.getEffectivePriceCents())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(pack.getEmoji() + " " + pack.getName()
                                                                        + ", " + pack.getTotalPoints() + " points Bruine")
                                                                .build())
                                                .build())
                                .build())
                .putMetadata("packId", String.valueOf(pack.getId()))
                .putMetadata("userId", String.valueOf(user.getId()))
                .build();

        return Session.create(params).getUrl();
    }

    /**
     * Finalise un achat après retour de Stripe : vérifie que la session est bien payée,
     * crédite les points et enregistre l'historique. Idempotent : une même session n'est
     * traitée qu'une seule fois (protège contre un rechargement de la page de succès).
     *
     * @return l'achat enregistré, ou {@link Optional#empty()} si non payé / déjà traité / introuvable
     */
    @Transactional
    public Optional<ShopPurchase> fulfillCheckout(String sessionId) throws StripeException {
        if (sessionId == null || sessionId.isBlank() || purchaseRepository.existsByStripeSessionId(sessionId)) {
            return Optional.empty();
        }

        Session session = Session.retrieve(sessionId);
        if (!"paid".equals(session.getPaymentStatus()) || session.getMetadata() == null) {
            return Optional.empty();
        }

        ShopPack pack = parseLong(session.getMetadata().get("packId"))
                .flatMap(packRepository::findById).orElse(null);
        SteamUser user = parseLong(session.getMetadata().get("userId"))
                .flatMap(steamUserService::findById).orElse(null);
        if (pack == null || user == null) {
            return Optional.empty();
        }

        int total = pack.getTotalPoints();
        user.setScore(user.getScore() + total);
        steamUserService.save(user);

        ShopPurchase purchase = new ShopPurchase();
        purchase.setSteamUser(user);
        purchase.setPackName(pack.getName());
        purchase.setPackEmoji(pack.getEmoji());
        purchase.setPointsCredited(total);
        purchase.setPriceCentsPaid(session.getAmountTotal() != null
                ? session.getAmountTotal().intValue() : pack.getEffectivePriceCents());
        purchase.setPromoPercent(pack.isPromoActive() ? pack.getPromoPercent() : 0);
        purchase.setStripeSessionId(sessionId);
        return Optional.of(purchaseRepository.save(purchase));
    }

    private Optional<Long> parseLong(String value) {
        try {
            return value == null ? Optional.empty() : Optional.of(Long.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Historique d'un utilisateur donné (le plus récent d'abord).
     */
    @Transactional(readOnly = true)
    public List<ShopPurchase> findUserHistory(Long userId) {
        return purchaseRepository.findBySteamUser_IdOrderByCreatedAtDesc(userId);
    }

    /**
     * Tous les achats (vue admin).
     */
    @Transactional(readOnly = true)
    public List<ShopPurchase> findAllHistory() {
        return purchaseRepository.findAllByOrderByCreatedAtDesc();
    }
}
