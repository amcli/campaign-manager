package com.dnd.campaignmanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name; //name of Character

    @NotBlank
    private String alias; //alias of Character

    @NotBlank
    private String faction;

    @NotBlank
    private String race;

    private int level;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacterStats> stats = new ArrayList<>();

    public void addStat(CharacterStats stat){
        stats.add(stat);
        stat.setCharacter(this);
    }



}
