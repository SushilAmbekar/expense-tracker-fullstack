import { addMonths, currentMonth, daysInMonth, firstDay, lastDay, parseIsoDate, toIsoDate } from './months';

describe('months', () => {
  it('formats and parses dates without time zone shifts', () => {
    expect(toIsoDate(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(toIsoDate(parseIsoDate('2026-11-30'))).toBe('2026-11-30');
  });

  it('adds months across year boundaries', () => {
    expect(addMonths('2026-11', 2)).toBe('2027-01');
    expect(addMonths('2026-01', -1)).toBe('2025-12');
  });

  it('knows the first and last day of a month', () => {
    expect(firstDay('2026-02')).toBe('2026-02-01');
    expect(lastDay('2026-02')).toBe('2026-02-28');
    expect(lastDay('2028-02')).toBe('2028-02-29');
    expect(daysInMonth('2026-10')).toBe(31);
  });

  it('gives the current month', () => {
    expect(currentMonth(new Date(2026, 10, 23))).toBe('2026-11');
  });
});
