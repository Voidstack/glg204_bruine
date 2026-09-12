package com.enosistudio.bruine.gacha.service;

import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional(readOnly = true)
    public List<GachaReward> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<GachaReward> findByRarity(ECardRarity rarity) {
        return repository.findByRarity(rarity);
    }

    @Transactional(readOnly = true)
    public Optional<GachaReward> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public GachaReward save(GachaReward reward) {
        return repository.save(reward);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
