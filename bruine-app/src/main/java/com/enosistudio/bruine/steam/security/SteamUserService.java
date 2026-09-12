package com.enosistudio.bruine.steam.security;

import com.enosistudio.bruine.steam.model.SteamUser;
import com.enosistudio.bruine.steam.repository.SteamUserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SteamUserService {

    private final SteamUserRepository repository;

    public SteamUserService(SteamUserRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<SteamUser> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<SteamUser> findBySteamId(String steamId) {
        return repository.findBySteamId(steamId);
    }

    @Transactional(readOnly = true)
    public Optional<SteamUser> findBySteamIdWithRewards(String steamId) {
        return repository.findWithRewardsBySteamId(steamId);
    }

    @Transactional
    public SteamUser save(SteamUser user) {
        return repository.save(user);
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