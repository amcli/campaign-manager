export type GameSystemCode =
  | 'DND_5E'
  | 'PATHFINDER_2E'
  | 'MUTANTS_AND_MASTERMINDS_3E'
  | 'DELTA_GREEN'
  | 'CALL_OF_CTHULHU_7E'
  | 'GENERIC';

export type FieldType = 'TEXT' | 'LONG_TEXT' | 'NUMBER' | 'BOOLEAN';

export interface SheetField {
  key: string;
  label: string;
  type: FieldType;
}

export interface SheetSection {
  name: string;
  fields: SheetField[];
}

export interface GameSystem {
  code: GameSystemCode;
  name: string;
  gameMasterTitle: string;
  sections: SheetSection[];
}

export type Sheet = Record<string, string>;

export interface UserSummary {
  id: number;
  username: string;
}

export type CampaignStatus = 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED';
export const CAMPAIGN_STATUSES: CampaignStatus[] = ['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED'];

export type NoteCategory = 'SESSION_PLAN' | 'PLOT' | 'NPC' | 'LOCATION' | 'LORE' | 'OTHER';
export const NOTE_CATEGORIES: NoteCategory[] = ['SESSION_PLAN', 'PLOT', 'NPC', 'LOCATION', 'LORE', 'OTHER'];

export type ConstraintType =
  | 'MAX_VALUE'
  | 'MIN_VALUE'
  | 'MAX_TOTAL'
  | 'MAX_ENTRIES'
  | 'MAX_REPEATS'
  | 'FORBIDDEN_TEXT';

export interface CampaignSummary {
  id: number;
  name: string;
  gameSystem: GameSystemCode;
  gameSystemName: string;
  gameMasterTitle: string;
  status: CampaignStatus;
  dungeonMaster: UserSummary;
  playerCount: number;
  maxPlayers: number | null;
  updatedAt: string;
}

export interface Note {
  id: number;
  title: string;
  content: string | null;
  category: NoteCategory;
  sharedWithPlayers: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BuildConstraint {
  id: number;
  type: ConstraintType;
  section: string | null;
  fieldKey: string | null;
  limit: number | null;
  text: string | null;
  description: string;
}

export interface CampaignCharacterEntry {
  id: number;
  name: string;
  owner: UserSummary;
  buildViolations: string[];
}

export interface CharacterOption {
  id: number;
  name: string;
}

export interface PendingInvite {
  id: number;
  characterId: number;
  characterName: string;
  player: UserSummary;
  createdAt: string;
}

export interface IncomingInvite {
  id: number;
  campaignId: number;
  campaignName: string;
  gameSystem: GameSystemCode;
  gameSystemName: string;
  gameMasterTitle: string;
  dungeonMaster: UserSummary;
  characterId: number;
  characterName: string;
  createdAt: string;
}

export interface CampaignDetail {
  id: number;
  name: string;
  description: string | null;
  gameSystem: GameSystemCode;
  gameSystemName: string;
  gameMasterTitle: string;
  status: CampaignStatus;
  maxPlayers: number | null;
  dungeonMaster: UserSummary;
  viewerIsDungeonMaster: boolean;
  players: UserSummary[];
  characters: CampaignCharacterEntry[];
  notes: Note[];
  buildConstraints: BuildConstraint[];
  pendingInvites: PendingInvite[];
  createdAt: string;
  updatedAt: string;
}

export interface CampaignRef {
  id: number;
  name: string;
  status: CampaignStatus;
}

export interface CharacterSummary {
  id: number;
  name: string;
  gameSystem: GameSystemCode;
  gameSystemName: string;
  campaign: CampaignRef | null;
  owner: UserSummary;
  updatedAt: string;
}

export interface CharacterDetail extends CharacterSummary {
  sheet: Sheet;
  buildViolations: string[];
  createdAt: string;
}

export interface CampaignInput {
  name: string;
  description: string;
  gameSystem: GameSystemCode;
  status: CampaignStatus;
  maxPlayers: number | null;
}

export interface NoteInput {
  title: string;
  content: string;
  category: NoteCategory;
  sharedWithPlayers: boolean;
}

export interface BuildConstraintInput {
  type: ConstraintType;
  section: string | null;
  fieldKey: string | null;
  limit: number | null;
  text: string | null;
}
