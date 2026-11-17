import { ChangeDetectionStrategy, Component } from '@angular/core';

// Marca del banco: la ficha de turno troquelada, con el corte punteado en ámbar.
@Component({
  selector: 'app-marca',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg class="marca__icono" viewBox="0 0 32 32" aria-hidden="true">
      <path fill="currentColor" d="M4 6a3 3 0 0 1 3-3h18a3 3 0 0 1 3 3v6a4 4 0 0 0 0 8v6a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3v-6a4 4 0 0 0 0-8z" />
      <path stroke="#f2b035" stroke-width="2" stroke-dasharray="2 2" d="M11 6v20" />
      <path fill="var(--fondo-marca, #14243a)" d="M15 12h9v2h-9zm0 6h6v2h-6z" />
    </svg>
    <span class="marca__texto">
      <span class="marca__nombre">Banco Visión</span>
      <span class="marca__sub">Turnos de sucursal</span>
    </span>
  `,
  styles: `
    :host { display: inline-flex; align-items: center; gap: 0.6rem; }
    .marca__icono { width: 2rem; height: 2rem; flex: none; }
    .marca__texto { display: grid; line-height: 1.15; }
    .marca__nombre { font-weight: 700; font-size: var(--t-l); }
    .marca__sub { font-size: var(--t-xs); opacity: 0.75; }
  `,
})
export class MarcaBanco {}
