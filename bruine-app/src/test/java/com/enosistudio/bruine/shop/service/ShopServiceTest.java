package com.enosistudio.bruine.shop.service;

import com.enosistudio.bruine.shop.model.ShopPack;
import com.enosistudio.bruine.shop.model.ShopPurchase;
import com.enosistudio.bruine.shop.repository.ShopPackRepository;
import com.enosistudio.bruine.shop.repository.ShopPurchaseRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import com.enosistudio.bruine.steam.security.SteamUserService;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Règles de la boutique côté service.
 * <p>
 * Le dialogue avec Stripe n'est pas joué ici : ce sont des appels statiques du SDK. Sont
 * vérifiées les décisions que la boutique prend seule, avant d'appeler Stripe ou sans
 * l'appeler du tout. Le calcul des prix vit sur l'entité et se teste dans ShopPackTest.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({ShopService.class, SteamUserService.class})
class ShopServiceTest {

    @Autowired
    private ShopService shopService;

    @Autowired
    private ShopPackRepository shopPackRepository;

    @Autowired
    private ShopPurchaseRepository shopPurchaseRepository;

    @Autowired
    private SteamUserRepository steamUserRepository;

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

    /**
     * La page de retour Stripe est une simple URL : le joueur peut la recharger, la mettre
     * en favori, y revenir le lendemain. Le deuxième passage ne doit rien créditer.
     */
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
