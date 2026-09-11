package com.dnd.campaignmanager.campaign;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByDungeonMasterIdOrderByUpdatedAtDesc(Long dungeonMasterId);

    List<Campaign> findByPlayersIdOrderByUpdatedAtDesc(Long playerId);
}
