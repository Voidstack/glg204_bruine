package com.enosistudio.bruine.deck;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.card.model.UserCard;
import com.enosistudio.bruine.steam.model.SteamUser;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * <img src="http://localhost:8080/deck/76561198100881386" alt="Deck Bruine">
 * ou
 * ![Deck Bruine](http://localhost:8080/deck/76561198100881386)
 */

@Component
public class DeckSvgRenderer {

    // Mise en page, en pixels.
    private static final int WIDTH = 740;
    private static final int PADDING = 18;
    private static final int GAP = 12;
    private static final int COLUMNS = 5;
    private static final int CARD_W = (WIDTH - 2 * PADDING - (COLUMNS - 1) * GAP) / COLUMNS;
    private static final int CARD_H = CARD_W;
    // Doivent rester synchronisés avec les hauteurs dessinées dans deck.svg
    private static final int HEADER_H = 74;
    private static final int STATS_H = 54;
    private static final int BADGE_W = 31;
    private static final int BADGE_H = 25;
    private static final int BADGE_GAP = 4;
    private static final int BADGE_MARGIN = 6;

    private final SpringTemplateEngine templateEngine;

    public DeckSvgRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * @param avatarDataUri avatar déjà encodé en {@code data:} URI, ou {@code null} s'il est indisponible
     */
    public String render(SteamUser user, List<UserCard> deckCards, String avatarDataUri) {
        int rows = Math.max(1, (int) Math.ceil(deckCards.size() / (double) COLUMNS));
        int gridTop = HEADER_H + STATS_H + PADDING;
        int height = gridTop + rows * CARD_H + (rows - 1) * GAP + PADDING;

        List<CardView> cards = new ArrayList<>();
        for (int i = 0; i < deckCards.size(); i++) {
            int x = PADDING + (i % COLUMNS) * (CARD_W + GAP);
            int y = gridTop + (i / COLUMNS) * (CARD_H + GAP);
            cards.add(CardView.of(i, x, y, deckCards.get(i)));
        }

        Context ctx = new Context();
        ctx.setVariable("height", height);
        ctx.setVariable("username", user.getUsername());
        ctx.setVariable("avatar", avatarDataUri);
        ctx.setVariable("playtime", playtimeLabel(user));
        ctx.setVariable("xp", user.getTotalExperience());
        ctx.setVariable("cards", cards);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + templateEngine.process("deck/svg/deck", ctx);
    }

    private static String playtimeLabel(SteamUser user) {
        Long minutes = user.getCurrentPlaytimeMinutes();
        return minutes != null ? (minutes / 60) + " h" : "Privé";
    }

    public record CardView(int index, int x, int y, int w, int h, String emoji, String background,
                           String rarityPip, int rarityTextX,
                           String finishPip, int finishTextX,
                           int badgeTextY) {

        static CardView of(int index, int x, int y, UserCard card) {
            ECardRarity rarity = card.getGachaReward().getRarity();
            // Comme sur fragments/card.html : pas de badge d'effet pour une carte normale
            String finishEmoji = card.getFinish() == ECardFinish.NORMAL ? null : card.getFinish().getEmoji();

            int badgeY = y + CARD_H - BADGE_MARGIN - BADGE_H;
            int rarityX = x + CARD_W - BADGE_MARGIN - BADGE_W;
            int finishX = rarityX - BADGE_GAP - BADGE_W;

            return new CardView(index, x, y, CARD_W, CARD_H, card.getGachaReward().getEmoji(),
                    rarity.getColor(),
                    rarity.getEmoji(), rarityX + BADGE_W / 2,
                    finishEmoji, finishX + BADGE_W / 2,
                    badgeY + BADGE_H - 6);
        }

        public int emojiX() {
            return x + w / 2;
        }

        public int emojiY() {
            return y + h / 2 + 14;
        }
    }
}
