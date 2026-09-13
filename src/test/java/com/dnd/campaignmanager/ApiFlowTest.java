package com.dnd.campaignmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession dmSession;
    private MockHttpSession playerSession;
    private String playerUsername;

    @BeforeEach
    void registerUsers() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        dmSession = register("dm_" + suffix);
        playerUsername = "player_" + suffix;
        playerSession = register(playerUsername);
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/campaigns/dm")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/characters")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", playerUsername, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gameSystemsExposeSheetTemplates() throws Exception {
        mockMvc.perform(get("/api/game-systems/DELTA_GREEN").session(playerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Delta Green"))
                .andExpect(jsonPath("$.gameMasterTitle").value("Handler"))
                .andExpect(jsonPath("$.sections[1].fields[4].key").value("power"));

        mockMvc.perform(get("/api/game-systems/NOT_A_SYSTEM").session(playerSession))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dungeonMasterSeesOnlyCampaignsTheyRun() throws Exception {
        long campaignId = createCampaign(dmSession, "Curse of Strahd", "DND_5E");

        mockMvc.perform(get("/api/campaigns/dm").session(dmSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(campaignId))
                .andExpect(jsonPath("$[0].gameSystemName").value("Dungeons & Dragons 5e"))
                .andExpect(jsonPath("$[0].status").value("PLANNING"));

        mockMvc.perform(get("/api/campaigns/dm").session(playerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/campaigns/" + campaignId).session(playerSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void characterListShowsCampaignMembership() throws Exception {
        long campaignId = createCampaign(dmSession, "Impossible Landscapes", "DELTA_GREEN");
        addPlayer(dmSession, campaignId, playerUsername);

        long agentId = createCharacter(playerSession, "Agent Marlowe", "DELTA_GREEN",
                Map.of("profession", "Federal Agent", "sanity", "62", "bonds", "Partner, ex-wife"));
        createCharacter(playerSession, "Sir Rowan", "PATHFINDER_2E", Map.of("ancestry", "Human"));

        mockMvc.perform(put("/api/characters/" + agentId + "/campaign").session(playerSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("campaignId", campaignId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaign.id").value(campaignId));

        mockMvc.perform(get("/api/characters").session(playerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.name == 'Agent Marlowe')].campaign.name").value("Impossible Landscapes"))
                .andExpect(jsonPath("$[?(@.name == 'Sir Rowan')].campaign").value(contains((Object) null)));

        mockMvc.perform(get("/api/campaigns/" + campaignId).session(dmSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characters.length()").value(1))
                .andExpect(jsonPath("$.characters[0].owner.username").value(playerUsername));

        mockMvc.perform(get("/api/characters").session(dmSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void characterCannotJoinCampaignOfAnotherSystem() throws Exception {
        long campaignId = createCampaign(dmSession, "Emerald Spire", "PATHFINDER_2E");
        addPlayer(dmSession, campaignId, playerUsername);
        long heroId = createCharacter(playerSession, "Captain Nova", "MUTANTS_AND_MASTERMINDS_3E", Map.of("powerLevel", "10"));

        mockMvc.perform(put("/api/characters/" + heroId + "/campaign").session(playerSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("campaignId", campaignId))))
                .andExpect(status().isConflict());
    }

    @Test
    void sheetValuesAreValidatedAgainstTheSystemTemplate() throws Exception {
        mockMvc.perform(post("/api/characters").session(playerSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Broken Sheet",
                                "gameSystem", "DND_5E",
                                "sheet", Map.of("strength", "lots", "sanity", "50")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.strength").value("Must be a whole number"))
                .andExpect(jsonPath("$.fieldErrors.sanity").value("Unknown field for this game system"));
    }

    @Test
    void onlyTheDungeonMasterManagesNotesAndPlayersSeeSharedOnes() throws Exception {
        long campaignId = createCampaign(dmSession, "Rise of the Runelords", "PATHFINDER_2E");
        addPlayer(dmSession, campaignId, playerUsername);

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/notes").session(dmSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Secret villain", "content", "Karzoug lives",
                                "category", "PLOT", "sharedWithPlayers", false))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/notes").session(dmSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Sandpoint", "content", "A quiet coastal town",
                                "category", "LOCATION", "sharedWithPlayers", true))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/notes").session(playerSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Nope", "category", "OTHER", "sharedWithPlayers", true))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/campaigns/" + campaignId).session(dmSession))
                .andExpect(jsonPath("$.notes.length()").value(2))
                .andExpect(jsonPath("$.viewerIsDungeonMaster").value(true));

        mockMvc.perform(get("/api/campaigns/" + campaignId).session(playerSession))
                .andExpect(jsonPath("$.notes.length()").value(1))
                .andExpect(jsonPath("$.notes[0].title").value("Sandpoint"))
                .andExpect(jsonPath("$.viewerIsDungeonMaster").value(false));
    }

    @Test
    void deletingACampaignReleasesItsCharacters() throws Exception {
        long campaignId = createCampaign(dmSession, "One Shot", "GENERIC");
        addPlayer(dmSession, campaignId, playerUsername);
        long characterId = createCharacter(playerSession, "Nameless", "GENERIC", Map.of());
        mockMvc.perform(put("/api/characters/" + characterId + "/campaign").session(playerSession).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("campaignId", campaignId))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/campaigns/" + campaignId).session(playerSession).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/campaigns/" + campaignId).session(dmSession).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/characters/" + characterId).session(playerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaign").isEmpty());
    }

    @Test
    void maxPlayersIsEnforcedAndCannotDropBelowCurrentCount() throws Exception {
        String secondPlayer = "second_" + UUID.randomUUID().toString().substring(0, 8);
        register(secondPlayer);

        long campaignId = idOf(mockMvc.perform(authed(post("/api/campaigns"), dmSession)
                        .content(json(Map.of("name", "Small Table", "gameSystem", "DND_5E", "maxPlayers", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maxPlayers").value(1))
                .andReturn());

        addPlayer(dmSession, campaignId, playerUsername);
        mockMvc.perform(authed(post("/api/campaigns/" + campaignId + "/players"), dmSession)
                        .content(json(Map.of("username", secondPlayer))))
                .andExpect(status().isConflict());

        mockMvc.perform(authed(put("/api/campaigns/" + campaignId), dmSession)
                        .content(json(Map.of("name", "Small Table", "status", "ACTIVE", "maxPlayers", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxPlayers").value(2));
        mockMvc.perform(authed(post("/api/campaigns/" + campaignId + "/players"), dmSession)
                        .content(json(Map.of("username", secondPlayer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players.length()").value(2));

        mockMvc.perform(authed(put("/api/campaigns/" + campaignId), dmSession)
                        .content(json(Map.of("name", "Small Table", "status", "ACTIVE", "maxPlayers", 1))))
                .andExpect(status().isConflict());
    }

    @Test
    void characterMustLeaveItsCampaignBeforeJoiningAnother() throws Exception {
        long first = createCampaign(dmSession, "First Table", "GENERIC");
        long second = createCampaign(dmSession, "Second Table", "GENERIC");
        addPlayer(dmSession, first, playerUsername);
        addPlayer(dmSession, second, playerUsername);
        long characterId = createCharacter(playerSession, "Wanderer", "GENERIC", Map.of());

        joinCampaign(playerSession, characterId, first).andExpect(status().isOk());
        joinCampaign(playerSession, characterId, second).andExpect(status().isConflict());

        mockMvc.perform(delete("/api/characters/" + characterId + "/campaign").session(playerSession).with(csrf()))
                .andExpect(status().isOk());
        joinCampaign(playerSession, characterId, second)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaign.id").value(second));
    }

    @Test
    void buildConstraintsGateJoiningAndSavingAndFlagExistingCharacters() throws Exception {
        long campaignId = createCampaign(dmSession, "Low Magic", "DND_5E");
        addPlayer(dmSession, campaignId, playerUsername);
        long strongId = createCharacter(playerSession, "Brutus", "DND_5E",
                Map.of("strength", "18", "equipment", "Longsword, Shield, Shield, Shield"));
        joinCampaign(playerSession, strongId, campaignId).andExpect(status().isOk());

        mockMvc.perform(authed(post("/api/campaigns/" + campaignId + "/constraints"), playerSession)
                        .content(json(Map.of("type", "MAX_VALUE", "fieldKey", "strength", "limit", 16))))
                .andExpect(status().isForbidden());
        mockMvc.perform(authed(post("/api/campaigns/" + campaignId + "/constraints"), dmSession)
                        .content(json(Map.of("type", "MAX_VALUE", "fieldKey", "sanity", "limit", 16))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fieldKey").exists());

        mockMvc.perform(authed(post("/api/campaigns/" + campaignId + "/constraints"), dmSession)
                        .content(json(Map.of("type", "MAX_VALUE", "section", "Ability Scores", "limit", 16))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Any Ability Scores field: at most 16"));
        mockMvc.perform(authed(post("/api/campaigns/" + campaignId + "/constraints"), dmSession)
                        .content(json(Map.of("type", "MAX_REPEATS", "fieldKey", "equipment", "limit", 2))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/campaigns/" + campaignId).session(dmSession))
                .andExpect(jsonPath("$.buildConstraints.length()").value(2))
                .andExpect(jsonPath("$.characters[0].buildViolations.length()").value(2))
                .andExpect(jsonPath("$.characters[0].buildViolations[0]").value("Strength is 18, limit is 16"));

        mockMvc.perform(authed(put("/api/characters/" + strongId), playerSession)
                        .content(json(Map.of("name", "Brutus", "sheet", Map.of("strength", "18", "equipment", "Longsword")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Strength is 18, limit is 16")));
        mockMvc.perform(authed(put("/api/characters/" + strongId), playerSession)
                        .content(json(Map.of("name", "Brutus", "sheet", Map.of("strength", "16", "equipment", "Longsword, Shield, shield")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildViolations.length()").value(0));

        long giantId = createCharacter(playerSession, "Goliath", "DND_5E", Map.of("constitution", "20"));
        joinCampaign(playerSession, giantId, campaignId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Constitution is 20, limit is 16")));
    }

    private ResultActions joinCampaign(MockHttpSession session, long characterId, long campaignId) throws Exception {
        return mockMvc.perform(authed(put("/api/characters/" + characterId + "/campaign"), session)
                .content(json(Map.of("campaignId", campaignId))));
    }

    @Test
    void dmCanInviteACharacterAndPlayerMustConfirmBeforeItJoins() throws Exception {
        long campaignId = createCampaign(dmSession, "Ghosts of Saltmarsh", "DND_5E");
        addPlayer(dmSession, campaignId, playerUsername);
        long characterId = createCharacter(playerSession, "Finnegan", "DND_5E", Map.of());
        long playerId = idOf(mockMvc.perform(get("/api/auth/me").session(playerSession))
                .andExpect(status().isOk())
                .andReturn());

        mockMvc.perform(get("/api/campaigns/" + campaignId + "/players/" + playerId + "/characters").session(dmSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Finnegan"));

        long inviteId = idOf(mockMvc.perform(authed(post("/api/campaigns/" + campaignId + "/invites"), dmSession)
                        .content(json(Map.of("characterId", characterId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.characterName").value("Finnegan"))
                .andReturn());

        // Not yet in the campaign: the invite is only a pending offer.
        mockMvc.perform(get("/api/characters/" + characterId).session(playerSession))
                .andExpect(jsonPath("$.campaign").isEmpty());
        mockMvc.perform(get("/api/campaigns/" + campaignId).session(dmSession))
                .andExpect(jsonPath("$.pendingInvites.length()").value(1))
                .andExpect(jsonPath("$.characters.length()").value(0));

        // A stranger cannot act on someone else's invite.
        mockMvc.perform(post("/api/invites/" + inviteId + "/accept").session(dmSession).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/invites").session(playerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].campaignName").value("Ghosts of Saltmarsh"))
                .andExpect(jsonPath("$[0].characterName").value("Finnegan"));

        mockMvc.perform(post("/api/invites/" + inviteId + "/accept").session(playerSession).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaign.id").value(campaignId));

        mockMvc.perform(get("/api/invites").session(playerSession))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/campaigns/" + campaignId).session(dmSession))
                .andExpect(jsonPath("$.pendingInvites.length()").value(0))
                .andExpect(jsonPath("$.characters.length()").value(1));
    }

    @Test
    void invitedCharacterCanBeDeclinedOrCancelledAndDmCannotInviteIneligibleCharacters() throws Exception {
        long campaignId = createCampaign(dmSession, "Tomb of Horrors", "DND_5E");
        addPlayer(dmSession, campaignId, playerUsername);
        long characterId = createCharacter(playerSession, "Vecna's Foe", "DND_5E", Map.of());

        mockMvc.perform(authed(post("/api/campaigns/" + campaignId + "/invites"), dmSession)
                        .content(json(Map.of("characterId", characterId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.notNullValue()));

        // Duplicate invite to the same campaign is rejected.
        mockMvc.perform(authed(post("/api/campaigns/" + campaignId + "/invites"), dmSession)
                        .content(json(Map.of("characterId", characterId))))
                .andExpect(status().isConflict());

        long inviteId = firstIdOf(mockMvc.perform(get("/api/invites").session(playerSession))
                .andExpect(status().isOk())
                .andReturn());
        mockMvc.perform(post("/api/invites/" + inviteId + "/decline").session(playerSession).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/invites").session(playerSession))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/characters/" + characterId).session(playerSession))
                .andExpect(jsonPath("$.campaign").isEmpty());

        // Not a player of this campaign: cannot be invited.
        String outsider = "outsider_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        MockHttpSession outsiderSession = register(outsider);
        long outsiderCharacterId = createCharacter(outsiderSession, "Rando", "DND_5E", Map.of());
        mockMvc.perform(authed(post("/api/campaigns/" + campaignId + "/invites"), dmSession)
                        .content(json(Map.of("characterId", outsiderCharacterId))))
                .andExpect(status().isConflict());

        // Only the DM can send or cancel invites for their own campaign.
        long secondInviteId = idOf(mockMvc.perform(authed(post("/api/campaigns/" + campaignId + "/invites"), dmSession)
                        .content(json(Map.of("characterId", characterId))))
                .andExpect(status().isCreated())
                .andReturn());
        mockMvc.perform(delete("/api/campaigns/" + campaignId + "/invites/" + secondInviteId).session(playerSession).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/campaigns/" + campaignId + "/invites/" + secondInviteId).session(dmSession).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/campaigns/" + campaignId).session(dmSession))
                .andExpect(jsonPath("$.pendingInvites.length()").value(0));
    }

    private MockHttpSession register(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "email", username + "@example.com", "password", "correct horse"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    private long createCampaign(MockHttpSession session, String name, String gameSystem) throws Exception {
        return idOf(mockMvc.perform(authed(post("/api/campaigns"), session)
                        .content(json(Map.of("name", name, "gameSystem", gameSystem, "description", "Test run"))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void addPlayer(MockHttpSession session, long campaignId, String username) throws Exception {
        mockMvc.perform(authed(post("/api/campaigns/" + campaignId + "/players"), session)
                        .content(json(Map.of("username", username))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players[0].username").value(username));
    }

    private long createCharacter(MockHttpSession session, String name, String gameSystem, Map<String, String> sheet) throws Exception {
        return idOf(mockMvc.perform(authed(post("/api/characters"), session)
                        .content(json(Map.of("name", name, "gameSystem", gameSystem, "sheet", sheet))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private static MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, MockHttpSession session) {
        return builder.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON);
    }

    private long idOf(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private long firstIdOf(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get(0).get("id").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
