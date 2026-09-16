package com.enosistudio.bruine.market.dto;

import com.enosistudio.bruine.card.dto.CardViewDTO;
import com.enosistudio.bruine.market.model.MarketListing;

/**
 * Une annonce du marché, prête à afficher : la carte passe par fragments/card.html.
 */
public record MarketListingDTO(Long id, int price, String sellerName, CardViewDTO card) {

    public static MarketListingDTO of(MarketListing listing) {
        return new MarketListingDTO(listing.getId(), listing.getPrice(),
                listing.getSeller().getUsername(), CardViewDTO.of(listing.getUserCard()));
    }
}
