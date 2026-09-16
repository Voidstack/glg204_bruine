package com.enosistudio.bruine.gacha.dto;

import com.enosistudio.bruine.card.dto.CardViewDTO;

import java.util.List;

/**
 * Résultat d'un tirage : les cartes obtenues, le score restant et le coût prélevé.
 */
public record GachaResultDTO(List<CardViewDTO> results, int score, int totalCost) {
}
