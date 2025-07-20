package com.dnd.campaignmanager.model;

import jakarta.persistence.*;
import lombok.Data;

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

    //finished Getter
    public Object getVal(){
        return switch(definition.getDataType()){
            case "STRING" -> stringValue;
            case "INTEGER" -> numberValue;
            case "BOOLEAN" -> booleanValue;
            default -> null;
        };
    }

}
