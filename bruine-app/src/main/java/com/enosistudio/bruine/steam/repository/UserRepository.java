package com.enosistudio.bruine.steam.repository;

import com.enosistudio.bruine.steam.model.SteamUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<SteamUser, Long> {

    Optional<SteamUser> findBySteamId(String steamId);

    @Query("SELECT DISTINCT u FROM SteamUser u LEFT JOIN FETCH u.cards c LEFT JOIN FETCH c.gachaReward WHERE u.steamId = :steamId")
    Optional<SteamUser> findBySteamIdWithRewards(@Param("steamId") String steamId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO user_card (steam_user_id, gacha_reward_id, finish) VALUES (:userId, :rewardId, :finish)", nativeQuery = true)
    void addCard(@Param("userId") Long userId, @Param("rewardId") Long rewardId, @Param("finish") String finish);

    @org.springframework.data.jpa.repository.Query(
        "SELECT u FROM SteamUser u ORDER BY COALESCE(u.currentPlaytimeMinutes, 0) DESC")
    java.util.List<SteamUser> findAllOrderByPlaytimeDesc();

    @org.springframework.data.jpa.repository.Query(
        "SELECT u FROM SteamUser u ORDER BY u.totalExperience DESC")
    java.util.List<SteamUser> findAllOrderByXpDesc();

    @Query("SELECT SUM(u.totalPulls) FROM SteamUser u")
    Long sumTotalPulls();

    @Query("SELECT SUM(u.totalExperience) FROM SteamUser u")
    Long sumTotalExperience();
}
