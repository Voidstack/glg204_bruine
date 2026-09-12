package com.enosistudio.bruine.gacha.service;

import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Catalogue des récompenses gacha : CRUD utilisé par l'administration et le tirage.
 */
@Service
public class GachaRewardService {

    private final GachaRewardRepository repository;

    public GachaRewardService(GachaRewardRepository repository) {
        this.repository = repository;
    }

    public List<GachaReward> findAll() {
        return repository.findAll();
    }

    public List<GachaReward> findByRarity(ECardRarity rarity) {
        return repository.findByRarity(rarity);
    }

    public Optional<GachaReward> findById(Long id) {
        return repository.findById(id);
    }

    public GachaReward save(GachaReward reward) {
        return repository.save(reward);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
