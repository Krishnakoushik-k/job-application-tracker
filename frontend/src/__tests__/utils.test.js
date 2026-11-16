import { describe, expect, it } from 'vitest';
import { blankToNull, formatDate, formatDateTime, formatMonth, toDateTimeInput } from '../utils/format';
import { hasErrors, validateApplication, validatePasswordChange, validateRegister } from '../utils/validators';

describe('format helpers', () => {
  it('formats ISO dates without shifting the day', () => {
    expect(formatDate('2026-11-10')).toBe('10 Nov 2026');
    expect(formatDate('2026-01-01')).toBe('1 Jan 2026');
    expect(formatDate(null)).toBe('—');
  });

  it('formats local date-times and months', () => {
    expect(formatDateTime('2026-11-20T14:30:00')).toBe('20 Nov 2026, 14:30');
    expect(formatMonth('2026-03')).toBe('Mar 2026');
  });

  it('prepares values for inputs and requests', () => {
    expect(toDateTimeInput('2026-11-20T14:30:00')).toBe('2026-11-20T14:30');
    expect(blankToNull('   ')).toBeNull();
    expect(blankToNull(' hi ')).toBe('hi');
  });
});

describe('validators', () => {
  it('accepts a valid registration and rejects bad passwords', () => {
    const valid = { fullName: 'Asha', email: 'a@b.co', password: 'Secret123', confirmPassword: 'Secret123' };
    expect(hasErrors(validateRegister(valid))).toBe(false);
    expect(validateRegister({ ...valid, password: 'onlyletters', confirmPassword: 'onlyletters' }).password).toBeDefined();
    expect(validateRegister({ ...valid, password: '12345678', confirmPassword: '12345678' }).password).toBeDefined();
  });

  it('validates password change', () => {
    const errors = validatePasswordChange({ currentPassword: '', newPassword: 'x', confirmPassword: 'y' });
    expect(errors.currentPassword).toBeDefined();
    expect(errors.newPassword).toBeDefined();
    expect(errors.confirmPassword).toBeDefined();
  });

  it('validates applications', () => {
    const base = { companyName: 'A', jobTitle: 'B', jobType: 'FULL_TIME', applicationDate: '2026-01-01', status: 'APPLIED' };
    expect(hasErrors(validateApplication(base))).toBe(false);
    expect(validateApplication({ ...base, companyName: '' }).companyName).toBeDefined();
    expect(validateApplication({ ...base, jobUrl: 'example.com' }).jobUrl).toBeDefined();
  });
});
