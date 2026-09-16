package com.enosistudio.bruine.level.dto;

import com.enosistudio.bruine.card.ECardFinish;

/**
 * Demande de conversion d'un exemplaire de carte, telle que l'envoie le navigateur.
 */
public record XpLevelConvertRequestDTO(Long rewardId, ECardFinish finish) {
}
