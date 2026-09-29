package com.dnd.campaignmanager.invite;

import com.dnd.campaignmanager.campaign.Campaign;
import com.dnd.campaignmanager.character.PlayerCharacter;
import com.dnd.campaignmanager.common.TimestampedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A pending offer from a campaign's game master for one specific character to join.
 * It sits until the character's owner accepts or declines; nothing ever expires it.
 */
@Entity
@Table(name = "campaign_invites", uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "character_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignInvite extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private PlayerCharacter character;

    public CampaignInvite(Campaign campaign, PlayerCharacter character) {
        this.campaign = campaign;
        this.character = character;
    }
}
