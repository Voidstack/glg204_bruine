package com.enosistudio.bruine.card.dto;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.gacha.model.GachaReward;

import java.util.List;

/**
 * Un empilement de cartes identiques dans la collection d'un joueur.
 * <p>
 * Deux exemplaires ne sont regroupés que s'ils partagent la même récompense et la même
 * finition, une finition différente étant une carte différente aux yeux du joueur.
 * <p>
 * La pile retient les identifiants de ses exemplaires, et non un simple total :
 * l'éditeur de deck a besoin de désigner un exemplaire précis quand le joueur en tire
 * un de la pile, alors que l'inventaire et la conversion ne regardent que le nombre.
 */
public record CardStackDTO(GachaReward reward, ECardFinish finish, List<Long> cardIds) {

    /**
     * Nombre d'exemplaires empilés.
     */
    public long count() {
        return cardIds.size();
    }

    /**
     * Modèle d'affichage passé au fragment de carte partagé.
     */
    public CardViewDTO view() {
        return CardViewDTO.of(reward, finish, count());
    }
}
