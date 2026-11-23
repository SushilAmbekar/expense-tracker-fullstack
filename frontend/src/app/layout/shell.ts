import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class ShellComponent {
  private readonly auth = inject(AuthService);

  protected readonly demo = environment.demo;
  protected readonly user = this.auth.user;
  protected readonly initials = computed(() =>
    (this.user()?.name ?? '?')
      .split(/\s+/)
      .map((part) => part[0])
      .slice(0, 2)
      .join('')
      .toUpperCase(),
  );
  protected readonly isHandset = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 899px)')
      .pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  protected readonly links = [
    { path: '/dashboard', icon: 'space_dashboard', label: 'Dashboard' },
    { path: '/expenses', icon: 'receipt_long', label: 'Expenses' },
    { path: '/budgets', icon: 'savings', label: 'Budgets' },
    { path: '/categories', icon: 'sell', label: 'Categories' },
  ];

  protected closeOnHandset(drawer: MatSidenav): void {
    if (this.isHandset()) {
      drawer.close();
    }
  }

  protected logout(): void {
    this.auth.logout();
  }
}
