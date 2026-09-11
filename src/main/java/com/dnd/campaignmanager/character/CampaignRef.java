package com.dnd.campaignmanager.character;

import com.dnd.campaignmanager.campaign.Campaign;
import com.dnd.campaignmanager.campaign.CampaignStatus;

public record CampaignRef(Long id, String name, CampaignStatus status) {

    public static CampaignRef from(Campaign campaign) {
        return campaign == null ? null : new CampaignRef(campaign.getId(), campaign.getName(), campaign.getStatus());
    }
}
