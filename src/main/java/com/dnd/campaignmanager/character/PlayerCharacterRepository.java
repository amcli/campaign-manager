package com.dnd.campaignmanager.character;

import com.dnd.campaignmanager.gamesystem.GameSystem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacter, Long> {

    List<PlayerCharacter> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);

    List<PlayerCharacter> findByCampaignIdOrderByNameAsc(Long campaignId);

    List<PlayerCharacter> findByCampaignIdAndOwnerId(Long campaignId, Long ownerId);

    List<PlayerCharacter> findByOwnerIdAndGameSystemAndCampaignIsNullOrderByNameAsc(Long ownerId, GameSystem gameSystem);
}
