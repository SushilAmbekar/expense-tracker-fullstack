import { Pipe, PipeTransform } from '@angular/core';

const whole = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const exact = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatInr(value: number | null | undefined, digits: 'whole' | 'exact' = 'whole'): string {
  return (digits === 'whole' ? whole : exact).format(value ?? 0);
}

/** Formats an amount in Indian rupees with Indian digit grouping, e.g. ₹1,25,000. */
@Pipe({ name: 'inr' })
export class InrPipe implements PipeTransform {
  transform(value: number | null | undefined, digits: 'whole' | 'exact' = 'whole'): string {
    return formatInr(value, digits);
  }
}
