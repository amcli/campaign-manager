package com.dnd.campaignmanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;

@Entity
@Data
public class CharacterStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stat_definition_id")
    private StatDefinition definition;

    private String stringValue;
    private Integer numberValue;
    private Boolean booleanValue;

    //implement Getter later
    public Object getVal(){
        //idk what this is supposed to do yet but i need to figure it out
    }

}
