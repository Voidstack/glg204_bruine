package com.enosistudio.bruine.market.service;

import com.enosistudio.bruine.common.BusinessRuleException;
import com.enosistudio.bruine.deck.model.UserCard;
import com.enosistudio.bruine.deck.repository.UserCardRepository;
import com.enosistudio.bruine.deck.service.DeckService;
import com.enosistudio.bruine.market.dto.MarketListingDTO;
import com.enosistudio.bruine.market.model.MarketListing;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class MarketService {

    private final MarketListingRepository marketListingRepository;
    private final UserCardRepository userCardRepository;
    private final SteamUserRepository steamUserRepository;
    private final DeckService deckService;

    public MarketService(MarketListingRepository marketListingRepository,
                         UserCardRepository userCardRepository,
                         SteamUserRepository steamUserRepository,
                         DeckService deckService) {
        this.marketListingRepository = marketListingRepository;
        this.userCardRepository = userCardRepository;
        this.steamUserRepository = steamUserRepository;
        this.deckService = deckService;
    }

    /**
     * Annonces des autres joueurs, les plus récentes d'abord.
     */
    @Transactional(readOnly = true)
    public List<MarketListingDTO> findOtherListings(Long userId) {
        return marketListingRepository.findBySellerIdNotOrderByCreatedAtDesc(userId).stream()
                .map(MarketListingDTO::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MarketListingDTO> findMyListings(Long sellerId) {
        return marketListingRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(MarketListingDTO::of)
                .toList();
    }

    /**
     * Cartes que le joueur peut mettre en vente : ni déjà en vente, ni dans son deck.
     *
     * @param user joueur chargé avec sa collection
     */
    @Transactional(readOnly = true)
    public List<UserCard> findSellableCards(SteamUser user) {
        Set<Long> listedIds = findAllListedCardIds();
        Set<Long> deckCardIds = deckService.findDeckCardIds(user.getId());
        return user.getCards().stream()
                .filter(card -> !listedIds.contains(card.getId()))
                .filter(card -> !deckCardIds.contains(card.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Set<Long> findAllListedCardIds() {
        return marketListingRepository.findAllListedCardIds();
    }

    @Transactional
    public void sell(SteamUser seller, Long cardId, int price) {
        if (price < 1) {
            throw new BusinessRuleException("Le prix doit être au moins 1 💧.");
        }

        UserCard card = userCardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessRuleException("Carte introuvable."));

        if (!card.getSteamUser().getId().equals(seller.getId())) {
            throw new BusinessRuleException("Cette carte ne vous appartient pas.");
        }

        // Une carte déjà en vente ne peut pas l'être deux fois. La contrainte d'unicité sur
        // market_listing.card_id le garantit de toute façon, mais elle ne sait pas le dire au
        // joueur : sans cette vérification, un double envoi du formulaire finit en erreur 500.
        if (marketListingRepository.existsByUserCardId(cardId)) {
            throw new BusinessRuleException("Cette carte est déjà en vente.");
        }

        // Bloquer la vente si cette carte précise est dans le deck
        Set<Long> deckCardIds = deckService.findDeckCardIds(seller.getId());
        if (deckCardIds.contains(card.getId())) {
            throw new BusinessRuleException(
                    "Cette carte est dans votre deck, retirez-la du deck avant de la vendre.");
        }

        MarketListing listing = new MarketListing();
        listing.setSeller(card.getSteamUser());
        listing.setUserCard(card);
        listing.setPrice(price);
        marketListingRepository.save(listing);
    }

    @Transactional
    public void buy(SteamUser buyer, Long listingId) {
        MarketListing listing = marketListingRepository.findById(listingId)
                .orElseThrow(() -> new BusinessRuleException("Annonce introuvable."));

        if (listing.getSeller().getId().equals(buyer.getId())) {
            throw new BusinessRuleException("Vous ne pouvez pas acheter votre propre carte.");
        }

        SteamUser managedBuyer = steamUserRepository.findById(buyer.getId()).orElseThrow();
        SteamUser managedSeller = steamUserRepository.findById(listing.getSeller().getId()).orElseThrow();

        if (managedBuyer.getScore() < listing.getPrice()) {
            throw new BusinessRuleException("Score insuffisant (il vous faut " + listing.getPrice() + " 💧).");
        }

        managedBuyer.setScore(managedBuyer.getScore() - listing.getPrice());
        managedSeller.setScore(managedSeller.getScore() + listing.getPrice());
        steamUserRepository.save(managedBuyer);
        steamUserRepository.save(managedSeller);

        userCardRepository.transferToNewOwner(listing.getUserCard().getId(), managedBuyer);
        marketListingRepository.deleteById(listingId);
    }

    @Transactional
    public void cancel(SteamUser seller, Long listingId) {
        MarketListing listing = marketListingRepository.findById(listingId)
                .orElseThrow(() -> new BusinessRuleException("Annonce introuvable."));

        if (!listing.getSeller().getId().equals(seller.getId())) {
            throw new BusinessRuleException("Vous ne pouvez pas annuler cette annonce.");
        }

        marketListingRepository.deleteById(listingId);
    }
}
