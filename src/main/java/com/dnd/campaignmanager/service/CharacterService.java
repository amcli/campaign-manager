package com.dnd.campaignmanager.service;

import com.dnd.campaignmanager.model.Character;
import com.dnd.campaignmanager.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CharacterService {
    private final CharacterRepository characterRepository;

    public List<Character> getAllChars(){
        return characterRepository.findAll();
    }

    //function to create a new character
    public Character createChar(Character character){
        return characterRepository.save(character);
    }

    public void deleteChar(Long id){
        characterRepository.deleteById(id);
    }

}