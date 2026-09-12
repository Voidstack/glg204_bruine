package com.enosistudio.bruine.steam.security;

import com.enosistudio.bruine.card.ECardFinish;
import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SteamUserService {

    private final UserRepository repository;

    public SteamUserService(UserRepository repository) {
        this.repository = repository;
    }

    public Optional<SteamUser> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<SteamUser> findBySteamId(String steamId) {
        return repository.findBySteamId(steamId);
    }

    public Optional<SteamUser> findBySteamIdWithRewards(String steamId) {
        return repository.findBySteamIdWithRewards(steamId);
    }

    public void addCard(Long userId, Long rewardId, ECardFinish finish) {
        repository.addCard(userId, rewardId, finish.name());
    }

    public SteamUser save(SteamUser user) {
        return repository.save(user);
    }

    public List<SteamUser> findAll() {
        return repository.findAll();
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<SteamUser> findLeaderboard() {
        return repository.findAllOrderByPlaytimeDesc();
    }

    public List<SteamUser> findLeaderboardByXp() {
        return repository.findAllOrderByXpDesc();
    }
}