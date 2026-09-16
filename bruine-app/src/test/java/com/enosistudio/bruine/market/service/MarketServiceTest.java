package com.enosistudio.bruine.market.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.model.UserCard;
import com.enosistudio.bruine.card.repository.UserCardRepository;
import com.enosistudio.bruine.card.service.UserCardService;
import com.enosistudio.bruine.common.BusinessRuleException;
import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import com.enosistudio.bruine.market.model.MarketListing;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({MarketService.class, DeckService.class, UserCardService.class, SteamUserService.class})
class MarketServiceTest {

    @Autowired
    private MarketService marketService;

    @Autowired
    private DeckService deckService;

    @Autowired
    private MarketListingRepository marketListingRepository;

    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private SteamUserRepository steamUserRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private GachaRewardRepository gachaRewardRepository;

    @Autowired
    private TestEntityManager entityManager;

    private SteamUser seller;
    private SteamUser buyer;
    private UserCard card;

    @BeforeEach
    void setUp() {
        seller = createUser("76561190000000001", 50);
        buyer = createUser("76561190000000002", 50);
        card = createCard(seller);
    }

    @Test
    void sellingPublishesTheListingAtTheAskedPrice() {
        marketService.sell(seller, card.getId(), 30);

        List<MarketListing> listings = marketListingRepository.findAll();
        assertEquals(1, listings.size());
        assertEquals(30, listings.get(0).getPrice());
        assertEquals(seller.getId(), listings.get(0).getSeller().getId());
        assertEquals(card.getId(), listings.get(0).getUserCard().getId());
    }

    @Test
    void sellingRefusesAPriceBelowOne() {
        assertThrows(BusinessRuleException.class, () -> marketService.sell(seller, card.getId(), 0));

        assertTrue(marketListingRepository.findAll().isEmpty());
    }

    @Test
    void sellingRefusesACardOwnedBySomeoneElse() {
        assertThrows(BusinessRuleException.class, () -> marketService.sell(buyer, card.getId(), 30));

        assertTrue(marketListingRepository.findAll().isEmpty());
    }

    @Test
    void sellingRefusesACardPlacedInTheDeck() {
        deckService.saveDeck(seller.getId(), List.of(card.getId()));

        assertThrows(BusinessRuleException.class, () -> marketService.sell(seller, card.getId(), 30));

        assertTrue(marketListingRepository.findAll().isEmpty());
    }

    @Test
    void sellingRefusesACardAlreadyOnSale() {
        publish(30);

        assertThrows(BusinessRuleException.class, () -> marketService.sell(seller, card.getId(), 20));

        assertEquals(1, marketListingRepository.findAll().size());
    }

    @Test
    void sellingAllowsAnotherCopyOfACardInTheDeck() {
        UserCard spare = createCard(seller);
        deckService.saveDeck(seller.getId(), List.of(card.getId()));

        marketService.sell(seller, spare.getId(), 30);

        assertEquals(1, marketListingRepository.findAll().size());
    }

    @Test
    void buyingMovesThePointsAndHandsOverTheCard() {
        Long listingId = publish(30);

        marketService.buy(buyer, listingId);
        entityManager.flush();
        entityManager.clear();

        assertEquals(20, steamUserRepository.findById(buyer.getId()).orElseThrow().getScore(),
                "l'acheteur paie le prix affiché");
        assertEquals(80, steamUserRepository.findById(seller.getId()).orElseThrow().getScore(),
                "le vendeur touche exactement ce prix");
        assertEquals(buyer.getId(),
                userCardRepository.findById(card.getId()).orElseThrow().getSteamUser().getId());
        assertTrue(marketListingRepository.findById(listingId).isEmpty());
    }

    @Test
    void buyingRefusesYourOwnListing() {
        Long listingId = publish(30);

        assertThrows(BusinessRuleException.class, () -> marketService.buy(seller, listingId));
    }

    @Test
    void buyingRefusesWhenTheScoreIsTooLow() {
        Long listingId = publish(80);

        assertThrows(BusinessRuleException.class, () -> marketService.buy(buyer, listingId));

        assertEquals(50, buyer.getScore());
        assertEquals(50, seller.getScore());
        assertEquals(seller.getId(),
                userCardRepository.findById(card.getId()).orElseThrow().getSteamUser().getId());
    }

    @Test
    void buyingAnUnknownListingIsRefused() {
        assertThrows(BusinessRuleException.class, () -> marketService.buy(buyer, 999999L));
    }

    @Test
    void cancellingWithdrawsTheListing() {
        Long listingId = publish(30);

        marketService.cancel(seller, listingId);

        assertTrue(marketListingRepository.findById(listingId).isEmpty());
    }

    @Test
    void cancellingRefusesSomeoneElsesListing() {
        Long listingId = publish(30);

        assertThrows(BusinessRuleException.class, () -> marketService.cancel(buyer, listingId));

        assertTrue(marketListingRepository.findById(listingId).isPresent());
    }

    private SteamUser createUser(String steamId, int score) {
        SteamUser user = new SteamUser();
        user.setSteamId(steamId);
        user.setUsername("joueur " + steamId);
        user.setScore(score);
        steamUserRepository.save(user);
        deckRepository.save(new Deck(user));
        return user;
    }

    private UserCard createCard(SteamUser holder) {
        GachaReward reward = new GachaReward();
        reward.setRarity(ECardRarity.EPIC);
        reward.setName("Dragon de brume");
        reward.setEmoji("*");
        gachaRewardRepository.save(reward);

        UserCard copy = new UserCard();
        copy.setSteamUser(holder);
        copy.setGachaReward(reward);
        copy.setFinish(ECardFinish.HOLOGRAPHIC);
        return userCardRepository.save(copy);
    }

    private Long publish(int price) {
        MarketListing listing = new MarketListing();
        listing.setUserCard(card);
        listing.setPrice(price);
        return marketListingRepository.save(listing).getId();
    }
}
