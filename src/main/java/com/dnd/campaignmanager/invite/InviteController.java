package com.dnd.campaignmanager.invite;

import com.dnd.campaignmanager.character.CharacterDetail;
import com.dnd.campaignmanager.character.PlayerCharacterService;
import com.dnd.campaignmanager.user.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The character owner's side of an invite: see what campaigns have offered a slot,
 * then accept or decline. Sending and cancelling an invite is a campaign action,
 * see CampaignController.
 */
@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final PlayerCharacterService characterService;

    @GetMapping
    public List<IncomingInvite> myInvites(@AuthenticationPrincipal AppUserPrincipal user) {
        return characterService.listMyInvites(user.id());
    }

    @PostMapping("/{id}/accept")
    public CharacterDetail accept(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        return characterService.acceptInvite(id, user.id());
    }

    @PostMapping("/{id}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        characterService.declineInvite(id, user.id());
    }
}
