import { InvalidPayloadError } from '../game/errors.js';

export function objectPayload(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new InvalidPayloadError();
  }
  return value as Record<string, unknown>;
}

export function requiredString(payload: Record<string, unknown>, field: string, maxLength: number): string {
  const value = payload[field];
  if (typeof value !== 'string' || value.trim().length === 0 || value.length > maxLength) {
    throw new InvalidPayloadError(`${field} không hợp lệ.`);
  }
  return value.trim();
}

export function optionalString(payload: Record<string, unknown>, field: string, maxLength: number): string | undefined {
  const value = payload[field];
  if (value === undefined) {
    return undefined;
  }
  if (typeof value !== 'string' || value.length === 0 || value.length > maxLength) {
    throw new InvalidPayloadError(`${field} không hợp lệ.`);
  }
  return value;
}
