package com.enosistudio.bruine.level.dto;

import com.enosistudio.bruine.card.CardStackDTO;

/**
 * Une pile de cartes libres, avec l'expérience qu'un exemplaire rapporte.
 */
public record XpConvertibleCardDTO(CardStackDTO stack, int xp) {
}
