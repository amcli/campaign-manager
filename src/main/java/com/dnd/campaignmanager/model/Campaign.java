package com.dnd.campaignmanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Entity
@Data
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String ruleSys;
    //this is to distinguish the rule sets between different campaigns like Delta Green, Pathfinder, M&M, DnD5e etc.


    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StatDefinition> statDef = new ArrayList<>();

}
