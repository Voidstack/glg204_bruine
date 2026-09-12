package com.enosistudio.bruine.market.service;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.common.BusinessRuleException;
import com.enosistudio.bruine.deck.model.UserCard;
import com.enosistudio.bruine.deck.repository.UserCardRepository;
import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import com.enosistudio.bruine.market.model.MarketListing;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Règles du marché entre joueurs.
 * <p>
 * Ces règles décident qui perd une carte et qui perd des points : une erreur y est
 * irréversible pour le joueur, contrairement à un défaut d'affichage.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({MarketService.class, DeckService.class})
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

    /**
     * Un deck vitrine ne doit pas pouvoir se vider sous les yeux de son propriétaire :
     * la carte posée dans le deck est bloquée tant qu'elle y est.
     */
    @Test
    void sellingRefusesACardPlacedInTheDeck() {
        deckService.saveDeck(seller.getId(), List.of(card.getId()));

        assertThrows(BusinessRuleException.class, () -> marketService.sell(seller, card.getId(), 30));

        assertTrue(marketListingRepository.findAll().isEmpty());
    }

    /**
     * Le blocage vise l'exemplaire posé, pas la carte : un doublon reste vendable.
     */
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

    /**
     * Le refus doit précéder tout mouvement : ni points débités, ni carte transférée.
     */
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
        return steamUserRepository.save(user);
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

    /**
     * Publie l'annonce du vendeur et renvoie son identifiant.
     */
    private Long publish(int price) {
        MarketListing listing = new MarketListing();
        listing.setSeller(seller);
        listing.setUserCard(card);
        listing.setPrice(price);
        return marketListingRepository.save(listing).getId();
    }
}
