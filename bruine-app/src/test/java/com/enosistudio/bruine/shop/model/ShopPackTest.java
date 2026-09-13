package com.enosistudio.bruine.shop.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopPackTest {

    @Test
    void theBonusCountsTowardsThePointsCredited() {
        assertEquals(1200, pack(499, 0).getTotalPoints());
    }

    @Test
    void withoutAPromoTheEffectivePriceIsTheCatalogPrice() {
        ShopPack plain = pack(499, 0);

        assertFalse(plain.isPromoActive());
        assertEquals(499, plain.getEffectivePriceCents());
        assertEquals("4.99", plain.getEffectivePriceEuros());
    }

    @Test
    void anActivePromoTakesItsPercentageOffThePrice() {
        ShopPack discounted = pack(500, 20);
        discounted.setPromoStart(LocalDateTime.now().minusDays(1));
        discounted.setPromoEnd(LocalDateTime.now().plusDays(1));

        assertTrue(discounted.isPromoActive());
        assertEquals(400, discounted.getEffectivePriceCents());
    }

    @Test
    void aPromoWithoutDatesIsAlwaysRunning() {
        assertTrue(pack(500, 20).isPromoActive());
    }

    @Test
    void aPromoIsIgnoredBeforeItStarts() {
        ShopPack notYet = pack(500, 20);
        notYet.setPromoStart(LocalDateTime.now().plusDays(1));

        assertFalse(notYet.isPromoActive());
        assertEquals(500, notYet.getEffectivePriceCents());
    }

    @Test
    void aPromoIsIgnoredOnceItIsOver() {
        ShopPack over = pack(500, 20);
        over.setPromoEnd(LocalDateTime.now().minusDays(1));

        assertFalse(over.isPromoActive());
        assertEquals(500, over.getEffectivePriceCents());
    }

    @Test
    void bothPricesAreFormattedWithTwoDecimals() {
        ShopPack discounted = pack(500, 20);

        assertEquals("5.00", discounted.getPriceEuros());
        assertEquals("4.00", discounted.getEffectivePriceEuros());
    }

    private ShopPack pack(int priceCents, int promoPercent) {
        ShopPack pack = new ShopPack();
        pack.setName("Seau de brume");
        pack.setPoints(1000);
        pack.setBonusPoints(200);
        pack.setPriceCents(priceCents);
        pack.setPromoPercent(promoPercent);
        return pack;
    }
}
