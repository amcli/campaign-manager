package com.dnd.campaignmanager.campaign;

import com.dnd.campaignmanager.character.CharacterOption;
import com.dnd.campaignmanager.character.PlayerCharacter;
import com.dnd.campaignmanager.character.PlayerCharacterRepository;
import com.dnd.campaignmanager.common.ConflictException;
import com.dnd.campaignmanager.common.ForbiddenException;
import com.dnd.campaignmanager.common.InvalidRequestException;
import com.dnd.campaignmanager.common.ResourceNotFoundException;
import com.dnd.campaignmanager.gamesystem.SheetTemplate;
import com.dnd.campaignmanager.invite.CampaignInvite;
import com.dnd.campaignmanager.invite.CampaignInviteRepository;
import com.dnd.campaignmanager.invite.PendingInvite;
import com.dnd.campaignmanager.user.User;
import com.dnd.campaignmanager.user.UserService;
import com.dnd.campaignmanager.user.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final PlayerCharacterRepository characterRepository;
    private final CampaignInviteRepository inviteRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<CampaignSummary> listRunBy(Long userId) {
        return campaignRepository.findByDungeonMasterIdOrderByUpdatedAtDesc(userId).stream()
                .map(CampaignSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CampaignSummary> listPlayedBy(Long userId) {
        return campaignRepository.findByPlayersIdOrderByUpdatedAtDesc(userId).stream()
                .map(CampaignSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CampaignDetail getDetail(Long campaignId, Long viewerId) {
        return toDetail(getVisibleTo(campaignId, viewerId), viewerId);
    }

    public CampaignDetail create(CampaignRequest request, Long dungeonMasterId) {
        User dungeonMaster = userService.getById(dungeonMasterId);
        Campaign campaign = new Campaign(
                request.name(),
                request.description(),
                request.gameSystem(),
                request.statusOrDefault(),
                request.maxPlayers(),
                dungeonMaster);
        return toDetail(campaignRepository.save(campaign), dungeonMasterId);
    }

    public CampaignDetail update(Long campaignId, CampaignUpdateRequest request, Long userId) {
        Campaign campaign = getRunBy(campaignId, userId);
        int currentPlayers = campaign.getPlayers().size();
        if (request.maxPlayers() != null && request.maxPlayers() < currentPlayers) {
            throw new ConflictException("Max players cannot be below the current player count of " + currentPlayers);
        }
        campaign.update(request.name(), request.description(), request.status(), request.maxPlayers());
        return toDetail(campaign, userId);
    }

    public void delete(Long campaignId, Long userId) {
        Campaign campaign = getRunBy(campaignId, userId);
        characterRepository.findByCampaignIdOrderByNameAsc(campaignId).forEach(PlayerCharacter::leaveCampaign);
        campaignRepository.delete(campaign);
    }

    public CampaignDetail addPlayer(Long campaignId, String username, Long userId) {
        Campaign campaign = getRunBy(campaignId, userId);
        User player = userService.getByUsername(username);
        if (campaign.isRunBy(player.getId())) {
            throw new ConflictException("The " + campaign.getGameSystem().getGameMasterTitle() + " cannot join as a player");
        }
        if (campaign.hasPlayer(player.getId())) {
            throw new ConflictException(player.getUsername() + " is already in this campaign");
        }
        if (campaign.isFull()) {
            throw new ConflictException("This campaign is full. Max players is " + campaign.getMaxPlayers());
        }
        campaign.addPlayer(player);
        return toDetail(campaign, userId);
    }

    public CampaignDetail removePlayer(Long campaignId, Long playerId, Long userId) {
        Campaign campaign = getRunBy(campaignId, userId);
        if (!campaign.hasPlayer(playerId)) {
            throw new ResourceNotFoundException("Player", playerId);
        }
        characterRepository.findByCampaignIdAndOwnerId(campaignId, playerId).forEach(PlayerCharacter::leaveCampaign);
        campaign.removePlayer(playerId);
        return toDetail(campaign, userId);
    }

    public NoteResponse addNote(Long campaignId, NoteRequest request, Long userId) {
        Campaign campaign = getRunBy(campaignId, userId);
        CampaignNote note = campaign.addNote(
                request.title(), request.content(), request.category(), request.sharedWithPlayers());
        campaignRepository.flush();
        return NoteResponse.from(note);
    }

    public NoteResponse updateNote(Long campaignId, Long noteId, NoteRequest request, Long userId) {
        CampaignNote note = getNote(getRunBy(campaignId, userId), noteId);
        note.update(request.title(), request.content(), request.category(), request.sharedWithPlayers());
        return NoteResponse.from(note);
    }

    public void deleteNote(Long campaignId, Long noteId, Long userId) {
        Campaign campaign = getRunBy(campaignId, userId);
        campaign.removeNote(getNote(campaign, noteId));
    }

    public BuildConstraintResponse addBuildConstraint(Long campaignId, BuildConstraintRequest request, Long userId) {
        Campaign campaign = getRunBy(campaignId, userId);
        String section = blankToNull(request.section());
        String fieldKey = blankToNull(request.fieldKey());
        String text = blankToNull(request.text());
        validateConstraint(campaign.sheetTemplate(), request.type(), section, fieldKey, request.limit(), text);

        BuildConstraint constraint = campaign.addBuildConstraint(request.type(), section, fieldKey, request.limit(), text);
        campaignRepository.flush();
        return BuildConstraintResponse.from(constraint, campaign.sheetTemplate());
    }

    public void removeBuildConstraint(Long campaignId, Long constraintId, Long userId) {
        Campaign campaign = getRunBy(campaignId, userId);
        BuildConstraint constraint = campaign.getBuildConstraints().stream()
                .filter(c -> c.getId().equals(constraintId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Build constraint", constraintId));
        campaign.removeBuildConstraint(constraint);
    }

    @Transactional(readOnly = true)
    public List<CharacterOption> eligibleCharactersForInvite(Long campaignId, Long playerId, Long dmUserId) {
        Campaign campaign = getRunBy(campaignId, dmUserId);
        if (!campaign.hasPlayer(playerId)) {
            throw new ResourceNotFoundException("Player", playerId);
        }
        return characterRepository
                .findByOwnerIdAndGameSystemAndCampaignIsNullOrderByNameAsc(playerId, campaign.getGameSystem())
                .stream()
                .filter(character -> !inviteRepository.existsByCampaignIdAndCharacterId(campaignId, character.getId()))
                .map(CharacterOption::from)
                .toList();
    }

    public PendingInvite inviteCharacter(Long campaignId, Long characterId, Long dmUserId) {
        Campaign campaign = getRunBy(campaignId, dmUserId);
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character", characterId));

        if (!campaign.hasPlayer(character.getOwner().getId())) {
            throw new ConflictException(character.getOwner().getUsername() + " is not a player in this campaign");
        }
        if (character.getGameSystem() != campaign.getGameSystem()) {
            throw new ConflictException("This character is built for " + character.getGameSystem().getDisplayName()
                    + " but the campaign runs " + campaign.getGameSystem().getDisplayName());
        }
        if (character.isInCampaign()) {
            throw new ConflictException(character.getName() + " is already in a campaign");
        }
        if (inviteRepository.existsByCampaignIdAndCharacterId(campaignId, characterId)) {
            throw new ConflictException(character.getName() + " already has a pending invite to this campaign");
        }

        return PendingInvite.from(inviteRepository.save(new CampaignInvite(campaign, character)));
    }

    public void cancelInvite(Long campaignId, Long inviteId, Long dmUserId) {
        getRunBy(campaignId, dmUserId);
        CampaignInvite invite = inviteRepository.findById(inviteId)
                .filter(candidate -> candidate.getCampaign().getId().equals(campaignId))
                .orElseThrow(() -> new ResourceNotFoundException("Invite", inviteId));
        inviteRepository.delete(invite);
    }

    public Campaign getVisibleTo(Long campaignId, Long viewerId) {
        Campaign campaign = getById(campaignId);
        if (!campaign.isVisibleTo(viewerId)) {
            throw new ForbiddenException("You are not part of this campaign");
        }
        return campaign;
    }

    private static void validateConstraint(SheetTemplate template, ConstraintType type, String section,
                                           String fieldKey, Integer limit, String text) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (section != null && template.sections().stream().noneMatch(s -> s.name().equals(section))) {
            errors.put("section", "Unknown section for this game system");
        }
        if (fieldKey != null && !template.fieldsByKey().containsKey(fieldKey)) {
            errors.put("fieldKey", "Unknown field for this game system");
        }
        if (type == ConstraintType.MAX_TOTAL && fieldKey != null) {
            errors.put("fieldKey", "Max total applies to a section or all stats, use Max value for one field");
        }
        if (type.isNeedsLimit() && (limit == null || limit < 0)) {
            errors.put("limit", "A limit of 0 or more is required");
        }
        if (type.isNeedsText() && text == null) {
            errors.put("text", "Text is required");
        }
        if (errors.isEmpty()) {
            BuildConstraint probe = new BuildConstraint(null, type, section, fieldKey, limit, text);
            if (probe.fieldsInScope(template).isEmpty()) {
                errors.put("type", "No fields of the right kind match this rule");
            }
        }
        if (!errors.isEmpty()) {
            throw new InvalidRequestException("Build constraint is not valid", errors);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Campaign getRunBy(Long campaignId, Long userId) {
        Campaign campaign = getById(campaignId);
        if (!campaign.isRunBy(userId)) {
            throw new ForbiddenException("Only the " + campaign.getGameSystem().getGameMasterTitle() + " can do this");
        }
        return campaign;
    }

    private Campaign getById(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
    }

    private static CampaignNote getNote(Campaign campaign, Long noteId) {
        return campaign.getNotes().stream()
                .filter(note -> note.getId().equals(noteId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Note", noteId));
    }

    private CampaignDetail toDetail(Campaign campaign, Long viewerId) {
        boolean viewerIsDungeonMaster = campaign.isRunBy(viewerId);
        SheetTemplate template = campaign.sheetTemplate();

        List<NoteResponse> notes = campaign.getNotes().stream()
                .filter(note -> viewerIsDungeonMaster || note.isSharedWithPlayers())
                .map(NoteResponse::from)
                .toList();

        List<CampaignCharacterEntry> characters = characterRepository.findByCampaignIdOrderByNameAsc(campaign.getId()).stream()
                .map(character -> new CampaignCharacterEntry(
                        character.getId(),
                        character.getName(),
                        UserSummary.from(character.getOwner()),
                        campaign.buildViolations(character.getSheet())))
                .toList();

        List<BuildConstraintResponse> constraints = campaign.getBuildConstraints().stream()
                .map(constraint -> BuildConstraintResponse.from(constraint, template))
                .toList();

        List<PendingInvite> pendingInvites = viewerIsDungeonMaster
                ? inviteRepository.findByCampaignIdOrderByCreatedAtAsc(campaign.getId()).stream()
                        .map(PendingInvite::from)
                        .toList()
                : List.of();

        return new CampaignDetail(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getGameSystem(),
                campaign.getGameSystem().getDisplayName(),
                campaign.getGameSystem().getGameMasterTitle(),
                campaign.getStatus(),
                campaign.getMaxPlayers(),
                UserSummary.from(campaign.getDungeonMaster()),
                viewerIsDungeonMaster,
                campaign.getPlayers().stream().map(UserSummary::from).toList(),
                characters,
                notes,
                constraints,
                pendingInvites,
                campaign.getCreatedAt(),
                campaign.getUpdatedAt());
    }
}
