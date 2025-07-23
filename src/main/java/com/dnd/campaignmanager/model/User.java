package com.dnd.campaignmanager.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import javax.management.relation.Role;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String username;

    @Email
    @NotBlank
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password; //figure out how to encrypt later

    @Enumerated(EnumType.STRING)
    private Role role = Role.PLAYER;

    @OneToMany(mappedBy = "dungeonMaster", cascade = CascadeType.ALL)
    private List<Campaign> campaignAsDM = new ArrayList<>();

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    private List<Character> characters = new ArrayList<>();

    @ManyToMany(mappedBy = "players")
    private List<Campaign> campaignsAsPlayer = new ArrayList<>();

    public enum Role {
        PLAYER, DUNGEON_MASTER, ADMIN
    }

}
