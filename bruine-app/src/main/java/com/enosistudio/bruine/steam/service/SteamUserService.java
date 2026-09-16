package com.enosistudio.bruine.steam.service;

import com.enosistudio.bruine.deck.model.Deck;
import com.enosistudio.bruine.deck.repository.DeckRepository;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class SteamUserService {

    /**
     * Une heure de jeu complète paie un tirage ({@code GachaService.COST_PER_PULL}).
     */
    public static final int POINTS_PER_PLAYED_HOUR = 10;

    private final SteamUserRepository repository;
    private final DeckRepository deckRepository;

    public SteamUserService(SteamUserRepository repository, DeckRepository deckRepository) {
        this.repository = repository;
        this.deckRepository = deckRepository;
    }

    @Transactional(readOnly = true)
    public Optional<SteamUser> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Relit le joueur et le verrouille jusqu'à la fin de la transaction de l'appelant.
     * <p>
     * À utiliser avant de modifier le score ou l'expérience : le joueur reçu d'un contrôleur a été
     * lu dans une autre transaction, ses valeurs peuvent déjà être périmées. Un verrou pris hors
     * transaction serait relâché aussitôt, d'où {@link Propagation#MANDATORY}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public SteamUser lock(Long id) {
        return repository.findForUpdateById(id)
                .orElseThrow(() -> new NoSuchElementException("Joueur introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public Optional<SteamUser> findBySteamId(String steamId) {
        return repository.findBySteamId(steamId);
    }

    @Transactional
    public SteamUser save(SteamUser user) {
        return repository.save(user);
    }

    /**
     * Enregistre une connexion : crée le joueur et son deck vide à sa première visite, met à jour sa date
     * de connexion et crédite chaque nouvelle heure de jeu Steam complète depuis la connexion précédente.
     *
     * @param totalPlaytimeMinutes temps de jeu Steam, null si le profil est privé
     */
    @Transactional
    public SteamUser recordLogin(String steamId, String username, Long totalPlaytimeMinutes) {
        SteamUser user = repository.findForUpdateBySteamId(steamId)
                .orElseGet(() -> new SteamUser(null, steamId, username));
        boolean firstVisit = user.getId() == null;
        user.setLastLoginAt(LocalDateTime.now());
        if (totalPlaytimeMinutes != null) {
            Long knownPlaytime = user.getCurrentPlaytimeMinutes();
            if (user.getInitialPlaytimeMinutes() == null) {
                user.setInitialPlaytimeMinutes(totalPlaytimeMinutes);
            }
            if (knownPlaytime == null) {
                user.setCurrentPlaytimeMinutes(totalPlaytimeMinutes);
            } else if (totalPlaytimeMinutes > knownPlaytime) {
                long newHours = totalPlaytimeMinutes / 60 - knownPlaytime / 60;
                long earned = newHours * POINTS_PER_PLAYED_HOUR;
                user.setScore(Math.toIntExact(user.getScore() + earned));
                user.setCurrentPlaytimeMinutes(totalPlaytimeMinutes);
            }
        }
        SteamUser saved = repository.save(user);
        if (firstVisit) {
            deckRepository.save(new Deck(saved));
        }
        return saved;
    }

    /**
     * Pour admin.
     */
    @Transactional
    public void updateScore(Long id, int score) {
        repository.findForUpdateById(id).ifPresent(user -> user.setScore(score));
    }

    @Transactional(readOnly = true)
    public List<SteamUser> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<SteamUser> findLeaderboard() {
        return repository.findAllByOrderByCurrentPlaytimeMinutesDesc();
    }

    @Transactional(readOnly = true)
    public List<SteamUser> findLeaderboardByXp() {
        return repository.findAllByOrderByTotalExperienceDesc();
    }
}
