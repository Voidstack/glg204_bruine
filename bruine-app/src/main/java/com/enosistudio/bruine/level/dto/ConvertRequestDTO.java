package com.enosistudio.bruine.level.dto;

/**
 * Demande de conversion d'un exemplaire de carte, telle que l'envoie le navigateur.
 */
public record ConvertRequestDTO(Long rewardId, String finish) {
}
