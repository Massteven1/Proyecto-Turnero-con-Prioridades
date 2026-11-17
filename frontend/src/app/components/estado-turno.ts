import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CLASE_CONJUNTO, CLASE_ESTADO, ETIQUETA_CONJUNTO, ETIQUETA_ESTADO } from '../services/etiquetas';
import { ConjuntoEfectivo, EstadoTurno } from '../services/modelos';

@Component({
  selector: 'app-estado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span [class]="'estado estado--' + clase()">{{ etiqueta() }}</span>`,
})
export class EstadoTurnoBadge {
  readonly estado = input.required<EstadoTurno>();
  protected readonly clase = computed(() => CLASE_ESTADO[this.estado()]);
  protected readonly etiqueta = computed(() => ETIQUETA_ESTADO[this.estado()]);
}

@Component({
  selector: 'app-conjunto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (conjunto(); as c) {
      <span [class]="'conjunto conjunto--' + clases[c]">{{ etiquetas[c] }}</span>
    }
  `,
})
export class ConjuntoBadge {
  readonly conjunto = input<ConjuntoEfectivo | null>(null);
  protected readonly clases = CLASE_CONJUNTO;
  protected readonly etiquetas = ETIQUETA_CONJUNTO;
}

@Component({
  selector: 'app-activo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span [class]="activo() ? 'estado estado--activo' : 'estado estado--inactivo'">{{
    activo() ? 'Activo' : 'Inactivo'
  }}</span>`,
})
export class ActivoBadge {
  readonly activo = input.required<boolean>();
}
