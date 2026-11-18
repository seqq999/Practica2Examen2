import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const CLAVE_SESION = 'insecure_pay_user';
const CLAVE_TOKEN = 'token';

function obtenerToken(): string | null {
  const directo = localStorage.getItem(CLAVE_TOKEN);
  if (directo) {
    return directo;
  }
  const sesion = localStorage.getItem(CLAVE_SESION);
  if (sesion) {
    try {
      return JSON.parse(sesion)?.token ?? null;
    } catch {
      return null;
    }
  }
  return null;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);
  const token = obtenerToken();

  let peticion = req;
  if (token) {
    peticion = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(peticion).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) {
        localStorage.removeItem(CLAVE_TOKEN);
        localStorage.removeItem('usuario');
        authService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
