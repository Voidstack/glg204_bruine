package com.enosistudio.bruine.steam.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.model.UserCard;
import com.enosistudio.bruine.card.repository.UserCardRepository;
import com.enosistudio.bruine.card.service.UserCardService;
import com.enosistudio.bruine.common.BusinessRuleException;
import com.enosistudio.bruine.common.InsufficientScoreException;
import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import com.enosistudio.bruine.gacha.service.GachaRewardService;
import com.enosistudio.bruine.gacha.service.GachaService;
import com.enosistudio.bruine.market.model.MarketListing;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import com.enosistudio.bruine.market.service.MarketService;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({MarketService.class, DeckService.class, UserCardService.class, GachaService.class, GachaRewardService.class, SteamUserService.class})
class ScoreConcurrencyTest {

    private static final int SIMULTANEOUS = 10;

    @Autowired
    private MarketService marketService;

    @Autowired
    private GachaService gachaService;

    @Autowired
    private SteamUserRepository steamUserRepository;

    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private GachaRewardRepository gachaRewardRepository;

    @Autowired
    private MarketListingRepository marketListingRepository;

    @AfterEach
    void cleanUp() {
        marketListingRepository.deleteAll();
        userCardRepository.deleteAll();
        steamUserRepository.deleteAll();
        gachaRewardRepository.deleteAll();
    }

    @Test
    void simultaneousPurchasesNeverSpendMoreThanTheBuyerOwns() throws Exception {
        SteamUser seller = createUser("76561190000000101", 0);
        SteamUser buyer = createUser("76561190000000102", 50);
        GachaReward reward = createReward();
        List<Long> listingIds = new ArrayList<>();
        for (int i = 0; i < SIMULTANEOUS; i++) {
            listingIds.add(publish(seller, createCard(seller, reward), 10));
        }

        AtomicInteger bought = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        runSimultaneously(i -> {
            try {
                marketService.buy(buyer, listingIds.get(i));
                bought.incrementAndGet();
            } catch (BusinessRuleException insufficientScore) {
                refused.incrementAndGet();
            }
        });

        SteamUser buyerAfter = steamUserRepository.findById(buyer.getId()).orElseThrow();
        SteamUser sellerAfter = steamUserRepository.findById(seller.getId()).orElseThrow();
        assertEquals(5, bought.get(), "50 💧 paient exactement cinq cartes à 10 💧");
        assertEquals(5, refused.get());
        assertEquals(0, buyerAfter.getScore(), "l'acheteur a tout dépensé, sans passer en négatif");
        assertEquals(50, sellerAfter.getScore(), "le vendeur touche chaque vente, aucune n'est écrasée");
        assertEquals(5, userCardRepository.findAll().stream()
                .filter(card -> card.getSteamUser().getId().equals(buyer.getId()))
                .count(), "l'acheteur reçoit une carte par achat payé");
    }

    @Test
    void simultaneousSpinsNeverSpendMoreThanThePlayerOwns() throws Exception {
        SteamUser player = createUser("76561190000000103", 50);

        AtomicInteger spun = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        runSimultaneously(i -> {
            try {
                gachaService.spin(player, 1);
                spun.incrementAndGet();
            } catch (InsufficientScoreException insufficientScore) {
                refused.incrementAndGet();
            }
        });

        SteamUser after = steamUserRepository.findById(player.getId()).orElseThrow();
        assertEquals(5, spun.get(), "50 💧 paient exactement cinq tirages à 10 💧");
        assertEquals(5, refused.get());
        assertEquals(0, after.getScore());
        assertEquals(5, after.getTotalPulls(), "chaque tirage payé est compté, aucun n'est écrasé");
    }

    private void runSimultaneously(IntConsumer operation) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(SIMULTANEOUS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < SIMULTANEOUS; i++) {
                int index = i;
                futures.add(pool.submit(() -> {
                    start.await();
                    operation.accept(index);
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private SteamUser createUser(String steamId, int score) {
        SteamUser user = new SteamUser();
        user.setSteamId(steamId);
        user.setUsername("joueur " + steamId);
        user.setScore(score);
        return steamUserRepository.save(user);
    }

    private GachaReward createReward() {
        GachaReward reward = new GachaReward();
        reward.setRarity(ECardRarity.EPIC);
        reward.setName("Dragon de brume");
        reward.setEmoji("*");
        return gachaRewardRepository.save(reward);
    }

    private UserCard createCard(SteamUser holder, GachaReward reward) {
        UserCard copy = new UserCard();
        copy.setSteamUser(holder);
        copy.setGachaReward(reward);
        copy.setFinish(ECardFinish.HOLOGRAPHIC);
        return userCardRepository.save(copy);
    }

    private Long publish(SteamUser seller, UserCard card, int price) {
        MarketListing listing = new MarketListing();
        listing.setUserCard(card);
        listing.setPrice(price);
        return marketListingRepository.save(listing).getId();
    }
}
