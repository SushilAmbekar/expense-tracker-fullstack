import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Only signed-in users can open the app's pages. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.token ? true : inject(Router).createUrlTree(['/login']);
};

/** Signed-in users skip the login and register pages. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.token ? inject(Router).createUrlTree(['/dashboard']) : true;
};
