import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

// Código PREFIJO-YYYYMMDD-NNN. El consecutivo va en negrita porque es la parte
// que el cliente compara con su ficha; prefijo y fecha quedan atenuados.
@Component({
  selector: 'app-codigo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (partes(); as p) {
      <span class="solo-lector">{{ codigo() }}</span>
      <span [class]="clase()" aria-hidden="true"
        ><span class="codigo__serie">{{ p.serie }}</span><span class="codigo__num">{{ p.numero }}</span></span
      >
    } @else {
      <span [class]="clase()">{{ codigo() }}</span>
    }
  `,
})
export class CodigoTurno {
  readonly codigo = input.required<string>();
  readonly tamano = input<'m' | 'l' | 'xl'>('m');

  protected readonly partes = computed(() => {
    const coincidencia = /^([A-Z]+-\d{8}-)(\d+)$/.exec(this.codigo() ?? '');
    return coincidencia ? { serie: coincidencia[1], numero: coincidencia[2] } : null;
  });

  protected readonly clase = computed(() =>
    this.tamano() === 'm' ? 'codigo' : `codigo codigo--${this.tamano()}`,
  );
}
