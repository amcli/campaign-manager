import { request } from './client';
import type {
  BuildConstraint,
  BuildConstraintInput,
  CampaignDetail,
  CampaignInput,
  CampaignSummary,
  CharacterDetail,
  CharacterSummary,
  GameSystem,
  GameSystemCode,
  Note,
  NoteInput,
  Sheet,
  UserSummary,
} from './types';

export const auth = {
  me: () => request<UserSummary>('GET', '/api/auth/me'),
  login: (username: string, password: string) =>
    request<UserSummary>('POST', '/api/auth/login', { username, password }),
  register: (username: string, email: string, password: string) =>
    request<UserSummary>('POST', '/api/auth/register', { username, email, password }),
  logout: () => request<void>('POST', '/api/auth/logout'),
  devLoginEnabled: () => request<{ enabled: boolean }>('GET', '/api/auth/dev-login-status'),
  devLogin: () => request<UserSummary>('POST', '/api/auth/dev-login'),
};

export const gameSystems = {
  list: () => request<GameSystem[]>('GET', '/api/game-systems'),
};

export const campaigns = {
  runByMe: () => request<CampaignSummary[]>('GET', '/api/campaigns/dm'),
  playedByMe: () => request<CampaignSummary[]>('GET', '/api/campaigns/playing'),
  get: (id: number) => request<CampaignDetail>('GET', `/api/campaigns/${id}`),
  create: (input: CampaignInput) => request<CampaignDetail>('POST', '/api/campaigns', input),
  update: (id: number, input: Omit<CampaignInput, 'gameSystem'>) =>
    request<CampaignDetail>('PUT', `/api/campaigns/${id}`, input),
  remove: (id: number) => request<void>('DELETE', `/api/campaigns/${id}`),
  addPlayer: (id: number, username: string) =>
    request<CampaignDetail>('POST', `/api/campaigns/${id}/players`, { username }),
  removePlayer: (id: number, playerId: number) =>
    request<CampaignDetail>('DELETE', `/api/campaigns/${id}/players/${playerId}`),
  addNote: (id: number, input: NoteInput) => request<Note>('POST', `/api/campaigns/${id}/notes`, input),
  updateNote: (id: number, noteId: number, input: NoteInput) =>
    request<Note>('PUT', `/api/campaigns/${id}/notes/${noteId}`, input),
  removeNote: (id: number, noteId: number) => request<void>('DELETE', `/api/campaigns/${id}/notes/${noteId}`),
  addConstraint: (id: number, input: BuildConstraintInput) =>
    request<BuildConstraint>('POST', `/api/campaigns/${id}/constraints`, input),
  removeConstraint: (id: number, constraintId: number) =>
    request<void>('DELETE', `/api/campaigns/${id}/constraints/${constraintId}`),
};

export const characters = {
  mine: () => request<CharacterSummary[]>('GET', '/api/characters'),
  get: (id: number) => request<CharacterDetail>('GET', `/api/characters/${id}`),
  create: (name: string, gameSystem: GameSystemCode, sheet: Sheet) =>
    request<CharacterDetail>('POST', '/api/characters', { name, gameSystem, sheet }),
  update: (id: number, name: string, sheet: Sheet) =>
    request<CharacterDetail>('PUT', `/api/characters/${id}`, { name, sheet }),
  remove: (id: number) => request<void>('DELETE', `/api/characters/${id}`),
  joinCampaign: (id: number, campaignId: number) =>
    request<CharacterDetail>('PUT', `/api/characters/${id}/campaign`, { campaignId }),
  leaveCampaign: (id: number) => request<CharacterDetail>('DELETE', `/api/characters/${id}/campaign`),
};
