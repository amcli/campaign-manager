package com.dnd.campaignmanager.service;

import com.dnd.campaignmanager.model.Campaign;
import com.dnd.campaignmanager.model.User;
import com.dnd.campaignmanager.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CampaignService {
    private final CampaignRepository campaignRepository;

    public List<Campaign> getAllCampaigns(){
        return campaignRepository.findAll();
    }

    public Optional<Campaign> getCampaignById(Long id) {
        return campaignRepository.findById(id);
    }

    public List<Campaign> getCampaignsByDM(Long dungeonMasterId) {
        return campaignRepository.findByDMId(dungeonMasterId);
    }

    public List<Campaign> getCampaignsByPlayer(Long playerId) {
        return campaignRepository.findByPlayersId(playerId);
    }

    public Campaign createCampaign(Campaign campaign) {
        return campaignRepository.save(campaign);
    }

    public Campaign updateCampaign(Campaign campaign) {
        return campaignRepository.save(campaign);
    }

    public void deleteCampaign(Long id) {
        campaignRepository.deleteById(id);
    }

    public Campaign addPlayerToCampaign(Long campaignId, User player) {
        Optional<Campaign> campaignOpt = campaignRepository.findById(campaignId);
        if (campaignOpt.isPresent()) {
            Campaign campaign = campaignOpt.get();
            campaign.getPlayers().add(player);
            return campaignRepository.save(campaign);
        }
        throw new RuntimeException("Campaign not found");
    }
}
