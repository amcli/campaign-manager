package com.dnd.campaignmanager.character;

import com.dnd.campaignmanager.campaign.Campaign;
import com.dnd.campaignmanager.campaign.CampaignService;
import com.dnd.campaignmanager.common.ConflictException;
import com.dnd.campaignmanager.common.ForbiddenException;
import com.dnd.campaignmanager.common.InvalidRequestException;
import com.dnd.campaignmanager.common.ResourceNotFoundException;
import com.dnd.campaignmanager.invite.CampaignInvite;
import com.dnd.campaignmanager.invite.CampaignInviteRepository;
import com.dnd.campaignmanager.invite.IncomingInvite;
import com.dnd.campaignmanager.user.User;
import com.dnd.campaignmanager.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerCharacterService {

    private final PlayerCharacterRepository characterRepository;
    private final CampaignInviteRepository inviteRepository;
    private final CampaignService campaignService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<CharacterSummary> listOwnedBy(Long userId) {
        return characterRepository.findByOwnerIdOrderByUpdatedAtDesc(userId).stream()
                .map(CharacterSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CharacterDetail get(Long characterId, Long viewerId) {
        PlayerCharacter character = getById(characterId);
        boolean viewerRunsItsCampaign = character.isInCampaign() && character.getCampaign().isRunBy(viewerId);
        if (!character.isOwnedBy(viewerId) && !viewerRunsItsCampaign) {
            throw new ForbiddenException("You cannot view this character");
        }
        return CharacterDetail.from(character);
    }

    public CharacterDetail create(CharacterRequest request, Long ownerId) {
        User owner = userService.getById(ownerId);
        Map<String, String> sheet = SheetValues.clean(request.gameSystem(), request.sheet());
        PlayerCharacter character = new PlayerCharacter(request.name(), request.gameSystem(), owner, sheet);
        return CharacterDetail.from(characterRepository.save(character));
    }

    public CharacterDetail update(Long characterId, CharacterUpdateRequest request, Long userId) {
        PlayerCharacter character = getOwnedBy(characterId, userId);
        Map<String, String> sheet = SheetValues.clean(character.getGameSystem(), request.sheet());
        if (character.isInCampaign()) {
            requireBuildAllowed(character.getCampaign(), sheet);
        }
        character.update(request.name(), sheet);
        return CharacterDetail.from(character);
    }

    public void delete(Long characterId, Long userId) {
        characterRepository.delete(getOwnedBy(characterId, userId));
    }

    public CharacterDetail joinCampaign(Long characterId, Long campaignId, Long userId) {
        PlayerCharacter character = getOwnedBy(characterId, userId);
        if (character.isInCampaign()) {
            throw new ConflictException("This character is already in " + character.getCampaign().getName()
                    + ". Leave that campaign first");
        }
        Campaign campaign = campaignService.getVisibleTo(campaignId, userId);
        if (campaign.getGameSystem() != character.getGameSystem()) {
            throw new ConflictException("This character is built for " + character.getGameSystem().getDisplayName()
                    + " but the campaign runs " + campaign.getGameSystem().getDisplayName());
        }
        requireBuildAllowed(campaign, character.getSheet());
        character.joinCampaign(campaign);
        inviteRepository.deleteAll(inviteRepository.findByCharacterId(character.getId()));
        return CharacterDetail.from(character);
    }

    public CharacterDetail leaveCampaign(Long characterId, Long userId) {
        PlayerCharacter character = getOwnedBy(characterId, userId);
        character.leaveCampaign();
        return CharacterDetail.from(character);
    }

    @Transactional(readOnly = true)
    public List<IncomingInvite> listMyInvites(Long userId) {
        return inviteRepository.findByCharacterOwnerIdOrderByCreatedAtAsc(userId).stream()
                .map(IncomingInvite::from)
                .toList();
    }

    public CharacterDetail acceptInvite(Long inviteId, Long userId) {
        CampaignInvite invite = getInviteAddressedTo(inviteId, userId);
        PlayerCharacter character = invite.getCharacter();
        if (character.isInCampaign()) {
            inviteRepository.delete(invite);
            throw new ConflictException(character.getName() + " is already in a campaign");
        }
        Campaign campaign = invite.getCampaign();
        requireBuildAllowed(campaign, character.getSheet());
        character.joinCampaign(campaign);
        inviteRepository.deleteAll(inviteRepository.findByCharacterId(character.getId()));
        return CharacterDetail.from(character);
    }

    public void declineInvite(Long inviteId, Long userId) {
        inviteRepository.delete(getInviteAddressedTo(inviteId, userId));
    }

    private CampaignInvite getInviteAddressedTo(Long inviteId, Long userId) {
        CampaignInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite", inviteId));
        if (!invite.getCharacter().isOwnedBy(userId)) {
            throw new ForbiddenException("This invite is not addressed to you");
        }
        return invite;
    }

    private static void requireBuildAllowed(Campaign campaign, Map<String, String> sheet) {
        List<String> violations = campaign.buildViolations(sheet);
        if (!violations.isEmpty()) {
            throw new InvalidRequestException(
                    "This build breaks the campaign rules: " + String.join("; ", violations));
        }
    }

    private PlayerCharacter getOwnedBy(Long characterId, Long userId) {
        PlayerCharacter character = getById(characterId);
        if (!character.isOwnedBy(userId)) {
            throw new ForbiddenException("You do not own this character");
        }
        return character;
    }

    private PlayerCharacter getById(Long characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character", characterId));
    }
}
