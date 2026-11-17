import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

// Panel de administración: catálogos, parámetros de cola y usuarios.
@Component({
  selector: 'app-admin',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="admin">
      <nav class="admin__nav" aria-label="Administración">
        <p class="admin__titulo">Administración</p>
        <a routerLink="servicios" routerLinkActive="activo" ariaCurrentWhenActive="page">Servicios</a>
        <a routerLink="modulos" routerLinkActive="activo" ariaCurrentWhenActive="page">Módulos de atención</a>
        <a routerLink="categorias" routerLinkActive="activo" ariaCurrentWhenActive="page">Categorías</a>
        <a routerLink="parametros" routerLinkActive="activo" ariaCurrentWhenActive="page">Parámetros de cola</a>
        <a routerLink="usuarios" routerLinkActive="activo" ariaCurrentWhenActive="page">Usuarios</a>
      </nav>
      <div class="admin__contenido">
        <router-outlet />
      </div>
    </div>
  `,
  styles: `
    .admin {
      display: grid;
      grid-template-columns: 13rem minmax(0, 1fr);
      gap: 2rem;
      align-items: start;
    }
    .admin__nav {
      position: sticky;
      top: 5rem;
      display: grid;
      gap: 0.15rem;
    }
    .admin__titulo {
      margin-bottom: 0.5rem;
      color: var(--tinta-tenue);
      font-size: var(--t-s);
      font-weight: 600;
    }
    .admin__nav a {
      padding: 0.55rem 0.75rem;
      border-left: 3px solid transparent;
      border-radius: 0 6px 6px 0;
      color: var(--tinta);
      font-weight: 600;
      text-decoration: none;
    }
    .admin__nav a:hover { background: var(--hoja); }
    .admin__nav a.activo {
      border-left-color: var(--sello);
      background: var(--hoja);
      color: var(--sello);
    }
    @media (max-width: 820px) {
      .admin { grid-template-columns: minmax(0, 1fr); gap: 1rem; }
      .admin__nav { position: static; display: flex; flex-wrap: wrap; }
      .admin__titulo { width: 100%; margin: 0; }
      .admin__nav a { border-left: 0; border-bottom: 3px solid transparent; border-radius: 6px 6px 0 0; }
      .admin__nav a.activo { border-bottom-color: var(--sello); }
    }
  `,
})
export class Admin {}
