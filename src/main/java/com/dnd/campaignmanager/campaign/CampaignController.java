package com.dnd.campaignmanager.campaign;

import com.dnd.campaignmanager.character.CharacterOption;
import com.dnd.campaignmanager.invite.InviteRequest;
import com.dnd.campaignmanager.invite.PendingInvite;
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
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping("/dm")
    public List<CampaignSummary> listRunByMe(@AuthenticationPrincipal AppUserPrincipal user) {
        return campaignService.listRunBy(user.id());
    }

    @GetMapping("/playing")
    public List<CampaignSummary> listPlayedByMe(@AuthenticationPrincipal AppUserPrincipal user) {
        return campaignService.listPlayedBy(user.id());
    }

    @GetMapping("/{id}")
    public CampaignDetail get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        return campaignService.getDetail(id, user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignDetail create(@Valid @RequestBody CampaignRequest request,
                                 @AuthenticationPrincipal AppUserPrincipal user) {
        return campaignService.create(request, user.id());
    }

    @PutMapping("/{id}")
    public CampaignDetail update(@PathVariable Long id,
                                 @Valid @RequestBody CampaignUpdateRequest request,
                                 @AuthenticationPrincipal AppUserPrincipal user) {
        return campaignService.update(id, request, user.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        campaignService.delete(id, user.id());
    }

    @PostMapping("/{id}/players")
    public CampaignDetail addPlayer(@PathVariable Long id,
                                    @Valid @RequestBody AddPlayerRequest request,
                                    @AuthenticationPrincipal AppUserPrincipal user) {
        return campaignService.addPlayer(id, request.username(), user.id());
    }

    @DeleteMapping("/{id}/players/{playerId}")
    public CampaignDetail removePlayer(@PathVariable Long id,
                                       @PathVariable Long playerId,
                                       @AuthenticationPrincipal AppUserPrincipal user) {
        return campaignService.removePlayer(id, playerId, user.id());
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse addNote(@PathVariable Long id,
                                @Valid @RequestBody NoteRequest request,
                                @AuthenticationPrincipal AppUserPrincipal user) {
        return campaignService.addNote(id, request, user.id());
    }

    @PutMapping("/{id}/notes/{noteId}")
    public NoteResponse updateNote(@PathVariable Long id,
                                   @PathVariable Long noteId,
                                   @Valid @RequestBody NoteRequest request,
                                   @AuthenticationPrincipal AppUserPrincipal user) {
        return campaignService.updateNote(id, noteId, request, user.id());
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@PathVariable Long id,
                           @PathVariable Long noteId,
                           @AuthenticationPrincipal AppUserPrincipal user) {
        campaignService.deleteNote(id, noteId, user.id());
    }

    @PostMapping("/{id}/constraints")
    @ResponseStatus(HttpStatus.CREATED)
    public BuildConstraintResponse addBuildConstraint(@PathVariable Long id,
                                                      @Valid @RequestBody BuildConstraintRequest request,
                                                      @AuthenticationPrincipal AppUserPrincipal user) {
        return campaignService.addBuildConstraint(id, request, user.id());
    }

    @DeleteMapping("/{id}/constraints/{constraintId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBuildConstraint(@PathVariable Long id,
                                      @PathVariable Long constraintId,
                                      @AuthenticationPrincipal AppUserPrincipal user) {
        campaignService.removeBuildConstraint(id, constraintId, user.id());
    }

    @GetMapping("/{id}/players/{playerId}/characters")
    public List<CharacterOption> eligibleCharacters(@PathVariable Long id,
                                                    @PathVariable Long playerId,
                                                    @AuthenticationPrincipal AppUserPrincipal user) {
        return campaignService.eligibleCharactersForInvite(id, playerId, user.id());
    }

    @PostMapping("/{id}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public PendingInvite inviteCharacter(@PathVariable Long id,
                                         @Valid @RequestBody InviteRequest request,
                                         @AuthenticationPrincipal AppUserPrincipal user) {
        return campaignService.inviteCharacter(id, request.characterId(), user.id());
    }

    @DeleteMapping("/{id}/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelInvite(@PathVariable Long id,
                             @PathVariable Long inviteId,
                             @AuthenticationPrincipal AppUserPrincipal user) {
        campaignService.cancelInvite(id, inviteId, user.id());
    }
}
