package com.dnd.campaignmanager.campaign;

import com.dnd.campaignmanager.common.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignNote extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 10000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoteCategory category;

    @Column(nullable = false)
    private boolean sharedWithPlayers;

    CampaignNote(Campaign campaign, String title, String content, NoteCategory category, boolean sharedWithPlayers) {
        this.campaign = campaign;
        this.title = title;
        this.content = content;
        this.category = category;
        this.sharedWithPlayers = sharedWithPlayers;
    }

    public void update(String title, String content, NoteCategory category, boolean sharedWithPlayers) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.sharedWithPlayers = sharedWithPlayers;
    }
}
