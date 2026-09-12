package com.enosistudio.bruine.card;

import com.enosistudio.bruine.gacha.model.GachaReward;

/**
 * Tout ce qu'il faut pour dessiner une carte.
 */
public record CardViewDTO(ECardRarity rarity, String emoji, String name, String description,
                          ECardFinish finish, long count) {

    /**
     * Emoji de la carte de repli, quand aucune carte n'existe pour la rareté tirée.
     */
    private static final String FALLBACK_EMOJI = "❓";

    public static CardViewDTO of(GachaReward reward, ECardFinish finish, long count) {
        return new CardViewDTO(reward.getRarity(), reward.getEmoji(), reward.getName(),
                reward.getDescription(), finish, count);
    }

    /**
     * Vue d'un exemplaire précis, par exemple une carte posée dans un deck.
     */
    public static CardViewDTO of(UserCard userCard) {
        return of(userCard.getGachaReward(), userCard.getFinish(), 1);
    }

    /**
     * Vue d'une carte tout juste tirée, donc toujours un exemplaire unique.
     * <p>
     * Le pool d'une rareté peut être vide si l'administrateur n'y a encore créé aucune
     * carte : on produit alors une carte de repli plutôt que de faire échouer le tirage.
     */
    public static CardViewDTO drawn(ECardRarity rarity, GachaReward reward, ECardFinish finish) {
        if (reward == null) {
            return new CardViewDTO(rarity, FALLBACK_EMOJI, rarity.getLabel(), null, finish, 1);
        }
        return of(reward, finish, 1);
    }

    /**
     * Vrai si la carte porte une finition spéciale, donc un badge et un effet.
     */
    public boolean hasFinish() {
        return finish != ECardFinish.NORMAL;
    }

    /**
     * Classes CSS de rareté et de finition, telles que les attend card.css.
     */
    public String cssClasses() {
        return hasFinish() ? rarity.getCode() + " finish-" + finish.getCode() : rarity.getCode();
    }
}
