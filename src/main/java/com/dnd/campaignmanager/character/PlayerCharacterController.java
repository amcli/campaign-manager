package com.dnd.campaignmanager.character;

import com.dnd.campaignmanager.user.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class PlayerCharacterController {

    private final PlayerCharacterService characterService;

    @GetMapping
    public List<CharacterSummary> listMine(@AuthenticationPrincipal AppUserPrincipal user) {
        return characterService.listOwnedBy(user.id());
    }

    @GetMapping("/{id}")
    public CharacterDetail get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        return characterService.get(id, user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CharacterDetail create(@Valid @RequestBody CharacterRequest request,
                                  @AuthenticationPrincipal AppUserPrincipal user) {
        return characterService.create(request, user.id());
    }

    @PutMapping("/{id}")
    public CharacterDetail update(@PathVariable Long id,
                                  @Valid @RequestBody CharacterUpdateRequest request,
                                  @AuthenticationPrincipal AppUserPrincipal user) {
        return characterService.update(id, request, user.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        characterService.delete(id, user.id());
    }

    @PutMapping("/{id}/campaign")
    public CharacterDetail joinCampaign(@PathVariable Long id,
                                        @Valid @RequestBody JoinCampaignRequest request,
                                        @AuthenticationPrincipal AppUserPrincipal user) {
        return characterService.joinCampaign(id, request.campaignId(), user.id());
    }

    @DeleteMapping("/{id}/campaign")
    public CharacterDetail leaveCampaign(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        return characterService.leaveCampaign(id, user.id());
    }
}
