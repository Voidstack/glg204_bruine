package com.enosistudio.bruine.market.service;

import com.enosistudio.bruine.card.dto.CardStackDTO;
import com.enosistudio.bruine.card.model.UserCard;
import com.enosistudio.bruine.card.service.UserCardService;
import com.enosistudio.bruine.common.BusinessRuleException;
import com.enosistudio.bruine.common.InsufficientScoreException;
import com.enosistudio.bruine.market.dto.MarketListingDTO;
import com.enosistudio.bruine.market.model.MarketListing;
import com.enosistudio.bruine.market.repository.MarketListingRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.service.SteamUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MarketService {

    private final MarketListingRepository marketListingRepository;
    private final SteamUserService steamUserService;
    private final UserCardService userCardService;

    public MarketService(MarketListingRepository marketListingRepository,
                         SteamUserService steamUserService,
                         UserCardService userCardService) {
        this.marketListingRepository = marketListingRepository;
        this.steamUserService = steamUserService;
        this.userCardService = userCardService;
    }

    /**
     * Annonces des autres joueurs, les plus récentes d'abord.
     */
    @Transactional(readOnly = true)
    public List<MarketListingDTO> findOtherListings(Long userId) {
        return marketListingRepository.findByUserCardSteamUserIdNotOrderByCreatedAtDesc(userId).stream()
                .map(MarketListingDTO::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MarketListingDTO> findMyListings(Long sellerId) {
        return marketListingRepository.findByUserCardSteamUserIdOrderByCreatedAtDesc(sellerId).stream()
                .map(MarketListingDTO::of)
                .toList();
    }

    /**
     * Cartes que le joueur peut mettre en vente : ni déjà en vente, ni dans son deck.
     */
    @Transactional(readOnly = true)
    public List<CardStackDTO> findSellableCards(Long userId) {
        return userCardService.findFreeCards(userId);
    }

    @Transactional
    public void sell(SteamUser seller, Long cardId, int price) {
        if (price < 1) {
            throw new BusinessRuleException("Le prix doit être au moins 1 💧.");
        }

        UserCard card = userCardService.requireFree(seller.getId(), cardId);

        MarketListing listing = new MarketListing();
        listing.setUserCard(card);
        listing.setPrice(price);
        marketListingRepository.save(listing);
    }

    @Transactional
    public void buy(SteamUser buyer, Long listingId) {
        // Verrou sur l'annonce d'abord : un achat concurrent de la même carte attend ici,
        // puis ne la trouve plus.
        MarketListing listing = marketListingRepository.findForUpdateById(listingId)
                .orElseThrow(() -> new BusinessRuleException("Annonce introuvable ou déjà vendue."));

        Long buyerId = buyer.getId();
        Long sellerId = listing.getSeller().getId();
        if (sellerId.equals(buyerId)) {
            throw new BusinessRuleException("Vous ne pouvez pas acheter votre propre carte.");
        }

        // Puis les deux joueurs, toujours dans l'ordre de leur id : deux achats croisés
        // (A achète à B pendant que B achète à A) ne peuvent pas s'interbloquer.
        SteamUser managedBuyer;
        SteamUser managedSeller;
        if (buyerId < sellerId) {
            managedBuyer = steamUserService.lock(buyerId);
            managedSeller = steamUserService.lock(sellerId);
        } else {
            managedSeller = steamUserService.lock(sellerId);
            managedBuyer = steamUserService.lock(buyerId);
        }

        if (managedBuyer.getScore() < listing.getPrice()) {
            throw new InsufficientScoreException(listing.getPrice());
        }

        // Les trois entités viennent d'être chargées dans cette transaction : Hibernate les
        // suit, poser les valeurs suffit, il écrit les UPDATE au flush sans save() explicite.
        managedBuyer.setScore(managedBuyer.getScore() - listing.getPrice());
        managedSeller.setScore(managedSeller.getScore() + listing.getPrice());
        listing.getUserCard().setSteamUser(managedBuyer);

        marketListingRepository.delete(listing);
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
