package com.enosistudio.bruine.steam.repository;

import com.enosistudio.bruine.steam.model.SteamUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Voir la doc
 * <a href="https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html">...</a>
 */
@Repository
public interface SteamUserRepository extends JpaRepository<SteamUser, Long> {

    Optional<SteamUser> findBySteamId(String steamId);

    /**
     * Joueur relu et verrouillé en écriture ({@code SELECT ... FOR UPDATE}) jusqu'à la fin de la
     * transaction. Toute modification de son score ou de son expérience passe par là : deux
     * opérations simultanées sur le même joueur s'exécutent l'une après l'autre au lieu que la
     * dernière écrite écrase l'autre.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SteamUser> findForUpdateById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SteamUser> findForUpdateBySteamId(String steamId);

    /**
     * Joueur et sa collection déjà chargée, pour les pages qui parcourent ses cartes.
     */
    @EntityGraph(attributePaths = {"cards", "cards.gachaReward"})
    Optional<SteamUser> findWithRewardsBySteamId(String steamId);

    /**
     * Classement par temps de jeu. Un temps inconnu (profil privé) vaut null et se range en fin
     * de liste, la base traitant null comme la plus petite valeur.
     */
    List<SteamUser> findAllByOrderByCurrentPlaytimeMinutesDesc();

    List<SteamUser> findAllByOrderByTotalExperienceDesc();

    @Query("SELECT SUM(u.totalPulls) FROM SteamUser u")
    Long sumTotalPulls();

    @Query("SELECT SUM(u.totalExperience) FROM SteamUser u")
    Long sumTotalExperience();
}
