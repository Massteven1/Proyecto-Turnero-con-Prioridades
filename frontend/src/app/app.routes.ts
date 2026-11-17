import { Routes } from '@angular/router';
import { authGuard, inicioGuard, invitadoGuard } from './guard/auth-guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', canActivate: [inicioGuard], children: [] },
  {
    path: 'login',
    title: 'Ingreso del personal | Banco Visión',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./modules/login/login').then((m) => m.Login),
  },
  {
    path: 'pantalla',
    title: 'Sala de espera | Banco Visión',
    loadComponent: () => import('./modules/pantalla/pantalla').then((m) => m.Pantalla),
  },
  {
    path: '',
    loadComponent: () => import('./components/main').then((m) => m.Main),
    canActivateChild: [authGuard],
    children: [
      {
        path: 'operacion',
        title: 'Ventanilla | Banco Visión',
        data: { roles: ['OPERADOR', 'ADMIN'] },
        loadComponent: () => import('./modules/operador/operador').then((m) => m.Operador),
      },
      {
        path: 'admin',
        data: { roles: ['ADMIN'] },
        loadComponent: () => import('./modules/admin/admin').then((m) => m.Admin),
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'servicios' },
          {
            path: 'servicios',
            title: 'Servicios | Banco Visión',
            loadComponent: () => import('./modules/admin/servicios').then((m) => m.Servicios),
          },
          {
            path: 'modulos',
            title: 'Módulos de atención | Banco Visión',
            loadComponent: () => import('./modules/admin/modulos').then((m) => m.Modulos),
          },
          {
            path: 'categorias',
            title: 'Categorías | Banco Visión',
            loadComponent: () => import('./modules/admin/categorias').then((m) => m.Categorias),
          },
          {
            path: 'parametros',
            title: 'Parámetros de cola | Banco Visión',
            loadComponent: () => import('./modules/admin/parametros').then((m) => m.Parametros),
          },
          {
            path: 'usuarios',
            title: 'Usuarios | Banco Visión',
            loadComponent: () => import('./modules/admin/usuarios').then((m) => m.Usuarios),
          },
        ],
      },
      {
        path: 'historial',
        title: 'Historial e indicadores | Banco Visión',
        data: { roles: ['ADMIN'] },
        loadComponent: () => import('./modules/historial/historial').then((m) => m.Historial),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
