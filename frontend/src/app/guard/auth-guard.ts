import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../services/modelos';
import { SesionService } from '../services/sesion';

// Exige sesión vigente y, si la ruta declara data.roles, que el rol coincida.
// Spring Security vuelve a validar todo en el servidor; esto solo ordena la
// navegación.
export const authGuard: CanActivateFn = (ruta, estado) => {
  const sesion = inject(SesionService);
  const router = inject(Router);

  if (!sesion.token()) {
    return router.createUrlTree(['/login'], { queryParams: { volver: estado.url } });
  }

  const roles = ruta.data['roles'] as Rol[] | undefined;
  const rol = sesion.rol();
  if (roles && (!rol || !roles.includes(rol))) {
    return router.parseUrl(sesion.rutaInicial());
  }
  return true;
};

// El login no tiene sentido con una sesión abierta.
export const invitadoGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  return sesion.token() ? inject(Router).parseUrl(sesion.rutaInicial()) : true;
};

export const inicioGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  return inject(Router).parseUrl(sesion.token() ? sesion.rutaInicial() : '/login');
};
