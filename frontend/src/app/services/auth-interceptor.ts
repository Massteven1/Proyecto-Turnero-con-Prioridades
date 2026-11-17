import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { API_URL } from './api-url';
import { SesionService } from './sesion';

// Rutas públicas del backend (permitAll): no llevan Authorization. La pantalla
// de sala de espera no depende de la identidad, así que un token vencido nunca
// debe acompañarla ni provocar un cierre de sesión.
export const RUTAS_SIN_TOKEN = ['/auth/login', '/publico/pantalla'];

export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const apiUrl = inject(API_URL);
  if (!peticion.url.startsWith(apiUrl)) {
    return siguiente(peticion);
  }

  const ruta = peticion.url.slice(apiUrl.length).split('?')[0];
  if (RUTAS_SIN_TOKEN.includes(ruta)) {
    return siguiente(peticion);
  }

  const sesion = inject(SesionService);
  const token = sesion.token();
  const conToken = token
    ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : peticion;

  return siguiente(conToken).pipe(
    catchError((error: unknown) => {
      // 401 en una ruta protegida: token vencido o cuenta desactivada / con
      // rol cambiado. El backend ya no lo acepta, así que se cierra la sesión.
      if (error instanceof HttpErrorResponse && error.status === 401 && token) {
        sesion.terminar('invalida');
      }
      return throwError(() => error);
    }),
  );
};
