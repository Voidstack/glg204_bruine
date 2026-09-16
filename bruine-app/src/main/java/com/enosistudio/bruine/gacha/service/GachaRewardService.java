package com.enosistudio.bruine.gacha.service;

import com.enosistudio.bruine.card.ECardRarity;
import com.enosistudio.bruine.gacha.dto.GachaRewardFormDTO;
import com.enosistudio.bruine.gacha.model.GachaReward;
import com.enosistudio.bruine.gacha.repository.GachaRewardRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Transactional
    public GachaReward create(GachaRewardFormDTO form) {
        GachaReward reward = new GachaReward();
        applyForm(reward, form);
        return repository.save(reward);
    }

    /**
     * Sans effet si la récompense a été supprimée entre-temps.
     */
    @Transactional
    public void update(Long id, GachaRewardFormDTO form) {
        repository.findById(id).ifPresent(reward -> applyForm(reward, form));
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private void applyForm(GachaReward reward, GachaRewardFormDTO form) {
        reward.setEmoji(form.emoji());
        reward.setName(form.name());
        reward.setRarity(form.rarity());
        reward.setDescription(form.description());
    }
}
