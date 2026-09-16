package com.enosistudio.bruine.shop.service;

import com.enosistudio.bruine.shop.model.ShopPack;
import com.enosistudio.bruine.shop.model.ShopPurchase;
import com.enosistudio.bruine.shop.repository.ShopPackRepository;
import com.enosistudio.bruine.shop.repository.ShopPurchaseRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import com.enosistudio.bruine.steam.service.SteamUserService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({ShopService.class, SteamUserService.class})
class ShopServiceTest {

    private static final String WEBHOOK_SECRET = "whsec_test_dummy";

    @Autowired
    private ShopService shopService;

    @Autowired
    private ShopPackRepository shopPackRepository;

    @Autowired
    private ShopPurchaseRepository shopPurchaseRepository;

    @Autowired
    private SteamUserRepository steamUserRepository;

    @Autowired
    private SteamUserService steamUserService;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void markingAPackAsPopularClearsTheFlagOnTheOthers() {
        ShopPack former = createPack("Ancien", true);
        ShopPack newcomer = createPack("Nouveau", false);
        newcomer.setPopular(true);

        shopService.save(newcomer);
        entityManager.flush();
        entityManager.clear();

        assertTrue(shopPackRepository.findById(newcomer.getId()).orElseThrow().isPopular());
        assertFalse(shopPackRepository.findById(former.getId()).orElseThrow().isPopular(),
                "un seul pack peut être mis en avant à la fois");
    }

    @Test
    void savingAnOrdinaryPackLeavesTheOthersAlone() {
        ShopPack popular = createPack("Populaire", true);
        ShopPack ordinary = createPack("Ordinaire", false);

        shopService.save(ordinary);
        entityManager.flush();
        entityManager.clear();

        assertTrue(shopPackRepository.findById(popular.getId()).orElseThrow().isPopular());
    }

    @Test
    void thePacksAreListedInTheOrderChosenByTheAdministrator() {
        ShopPack second = createPack("Second", false);
        second.setSortOrder(2);
        ShopPack first = createPack("Premier", false);
        first.setSortOrder(1);
        shopPackRepository.save(second);
        shopPackRepository.save(first);

        assertEquals("Premier", shopService.findAllOrdered().get(0).getName());
    }

    @Test
    void aPaymentAlreadyCreditedIsNotCreditedAgain() throws StripeException {
        SteamUser player = createPlayer(100);
        recordPurchase(player, "cs_test_1");

        assertTrue(shopService.fulfillCheckout("cs_test_1").isEmpty());

        assertEquals(100, player.getScore(), "le score ne bouge pas au second passage");
        assertEquals(1, shopPurchaseRepository.count(), "aucun achat en double n'est enregistré");
    }

    @Test
    void anAbsentSessionIdentifierCreditsNothing() throws StripeException {
        assertTrue(shopService.fulfillCheckout(null).isEmpty());
        assertTrue(shopService.fulfillCheckout("   ").isEmpty());

        assertEquals(0, shopPurchaseRepository.count());
    }

    @Test
    void aWebhookEventWithoutAValidSignatureIsRefused() {
        assertThrows(SignatureVerificationException.class,
                () -> shopService.paidCheckoutSessionId(checkoutEvent("checkout.session.completed"), "t=1,v1=invalide"));
    }

    @Test
    void theWebhookRefusesEveryEventWhenNoSecretIsConfigured() throws Exception {
        ShopService withoutSecret = new ShopService(shopPackRepository, shopPurchaseRepository, steamUserService, "");
        String payload = checkoutEvent("checkout.session.completed");

        assertThrows(SignatureVerificationException.class,
                () -> withoutSecret.paidCheckoutSessionId(payload, sign(payload)));
    }

    @Test
    void aSignedPaidCheckoutEventGivesTheSessionToFulfill() throws Exception {
        String payload = checkoutEvent("checkout.session.completed");

        assertEquals(Optional.of("cs_test_42"), shopService.paidCheckoutSessionId(payload, sign(payload)));
    }

    @Test
    void aSignedEventThatIsNotAPaymentIsIgnored() throws Exception {
        String payload = checkoutEvent("checkout.session.expired");

        assertTrue(shopService.paidCheckoutSessionId(payload, sign(payload)).isEmpty());
    }

    private String checkoutEvent(String type) {
        return """
                {"id": "evt_test_1", "object": "event", "type": "%s",
                 "data": {"object": {"id": "cs_test_42", "object": "checkout.session"}}}
                """.formatted(type);
    }

    private String sign(String payload) throws Exception {
        long timestamp = Webhook.Util.getTimeNow();
        return "t=" + timestamp + ",v1=" + Webhook.Util.computeHmacSha256(WEBHOOK_SECRET, timestamp + "." + payload);
    }

    private ShopPack createPack(String name, boolean popular) {
        ShopPack pack = new ShopPack();
        pack.setName(name);
        pack.setEmoji("*");
        pack.setPoints(1000);
        pack.setPriceCents(499);
        pack.setPopular(popular);
        return shopPackRepository.save(pack);
    }

    private SteamUser createPlayer(int score) {
        SteamUser player = new SteamUser();
        player.setSteamId("76561190000000001");
        player.setUsername("joueur");
        player.setScore(score);
        return steamUserRepository.save(player);
    }

    private void recordPurchase(SteamUser player, String sessionId) {
        ShopPurchase purchase = new ShopPurchase();
        purchase.setSteamUser(player);
        purchase.setPackName("Seau de brume");
        purchase.setPackEmoji("*");
        purchase.setPointsCredited(1000);
        purchase.setPriceCentsPaid(499);
        purchase.setStripeSessionId(sessionId);
        purchase.setCreatedAt(LocalDateTime.now());
        shopPurchaseRepository.save(purchase);
    }
}
