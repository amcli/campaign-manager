package com.dnd.campaignmanager.invite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignInviteRepository extends JpaRepository<CampaignInvite, Long> {

    List<CampaignInvite> findByCampaignIdOrderByCreatedAtAsc(Long campaignId);

    List<CampaignInvite> findByCharacterOwnerIdOrderByCreatedAtAsc(Long ownerId);

    List<CampaignInvite> findByCharacterId(Long characterId);

    boolean existsByCampaignIdAndCharacterId(Long campaignId, Long characterId);
}
