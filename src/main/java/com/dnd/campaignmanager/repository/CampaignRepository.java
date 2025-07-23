package com.dnd.campaignmanager.repository;

import com.dnd.campaignmanager.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByDMId(Long dungeonMasterId);
    List<Campaign> findByPlayersId(Long playerId);
}
