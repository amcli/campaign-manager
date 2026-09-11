package com.dnd.campaignmanager.character;

import com.dnd.campaignmanager.campaign.Campaign;
import com.dnd.campaignmanager.common.TimestampedEntity;
import com.dnd.campaignmanager.gamesystem.GameSystem;
import com.dnd.campaignmanager.user.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "characters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerCharacter extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GameSystem gameSystem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "character_sheet_values", joinColumns = @JoinColumn(name = "character_id"))
    @MapKeyColumn(name = "field_key", length = 64)
    @Column(name = "field_value", length = 5000)
    private Map<String, String> sheet = new LinkedHashMap<>();

    public PlayerCharacter(String name, GameSystem gameSystem, User owner, Map<String, String> sheet) {
        this.name = name;
        this.gameSystem = gameSystem;
        this.owner = owner;
        this.sheet = new LinkedHashMap<>(sheet);
    }

    public void update(String name, Map<String, String> sheet) {
        this.name = name;
        this.sheet.clear();
        this.sheet.putAll(sheet);
    }

    public boolean isOwnedBy(Long userId) {
        return owner.getId().equals(userId);
    }

    public boolean isInCampaign() {
        return campaign != null;
    }

    public void joinCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public void leaveCampaign() {
        this.campaign = null;
    }
}
