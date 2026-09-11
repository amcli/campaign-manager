package com.dnd.campaignmanager.campaign;

import com.dnd.campaignmanager.common.TimestampedEntity;
import com.dnd.campaignmanager.gamesystem.GameSystem;
import com.dnd.campaignmanager.gamesystem.SheetTemplate;
import com.dnd.campaignmanager.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Campaign extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GameSystem gameSystem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status;

    private Integer maxPlayers;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dungeon_master_id", nullable = false)
    private User dungeonMaster;

    @ManyToMany
    @JoinTable(
            name = "campaign_players",
            joinColumns = @JoinColumn(name = "campaign_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> players = new LinkedHashSet<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("updatedAt DESC")
    private List<CampaignNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<BuildConstraint> buildConstraints = new ArrayList<>();

    public Campaign(String name, String description, GameSystem gameSystem, CampaignStatus status,
                    Integer maxPlayers, User dungeonMaster) {
        this.name = name;
        this.description = description;
        this.gameSystem = gameSystem;
        this.status = status;
        this.maxPlayers = maxPlayers;
        this.dungeonMaster = dungeonMaster;
    }

    public void update(String name, String description, CampaignStatus status, Integer maxPlayers) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.maxPlayers = maxPlayers;
    }

    public boolean isRunBy(Long userId) {
        return dungeonMaster.getId().equals(userId);
    }

    public boolean hasPlayer(Long userId) {
        return players.stream().anyMatch(player -> player.getId().equals(userId));
    }

    public boolean isVisibleTo(Long userId) {
        return isRunBy(userId) || hasPlayer(userId);
    }

    public boolean isFull() {
        return maxPlayers != null && players.size() >= maxPlayers;
    }

    public void addPlayer(User player) {
        players.add(player);
    }

    public void removePlayer(Long userId) {
        players.removeIf(player -> player.getId().equals(userId));
    }

    public CampaignNote addNote(String title, String content, NoteCategory category, boolean sharedWithPlayers) {
        CampaignNote note = new CampaignNote(this, title, content, category, sharedWithPlayers);
        notes.add(note);
        return note;
    }

    public void removeNote(CampaignNote note) {
        notes.remove(note);
    }

    public BuildConstraint addBuildConstraint(ConstraintType type, String section, String fieldKey,
                                              Integer limit, String text) {
        BuildConstraint constraint = new BuildConstraint(this, type, section, fieldKey, limit, text);
        buildConstraints.add(constraint);
        return constraint;
    }

    public void removeBuildConstraint(BuildConstraint constraint) {
        buildConstraints.remove(constraint);
    }

    public SheetTemplate sheetTemplate() {
        return gameSystem.sheetTemplate();
    }

    public List<String> buildViolations(Map<String, String> sheet) {
        SheetTemplate template = sheetTemplate();
        return buildConstraints.stream()
                .flatMap(constraint -> constraint.violations(template, sheet).stream())
                .toList();
    }
}
