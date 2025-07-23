package com.dnd.campaignmanager.repository;

import com.dnd.campaignmanager.model.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CharacterRepository extends JpaRepository<Character, Long> {
    //this is a custom query used to find characters by their class
    List<Character> findByCharacterClass(String characterClass);


    List<Character> findByCharacterFaction(String characterFaction);
    List<Character> findByCharacterRace(String characterRace);
}
