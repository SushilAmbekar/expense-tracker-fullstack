import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { Observable, map } from 'rxjs';

export interface ConfirmData {
  title: string;
  message: string;
  confirmLabel?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>{{ data.message }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button class="danger" [mat-dialog-close]="true">{{ data.confirmLabel ?? 'Delete' }}</button>
    </mat-dialog-actions>
  `,
})
export class ConfirmDialogComponent {
  protected readonly data = inject<ConfirmData>(MAT_DIALOG_DATA);
}

/** Opens a confirmation dialog; emits true only when the user confirms. */
export function confirm(dialog: MatDialog, data: ConfirmData): Observable<boolean> {
  return dialog
    .open(ConfirmDialogComponent, { data, width: '420px' })
    .afterClosed()
    .pipe(map((result) => result === true));
}
