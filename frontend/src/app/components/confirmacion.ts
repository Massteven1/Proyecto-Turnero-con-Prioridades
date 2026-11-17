import { ChangeDetectionStrategy, Component, ElementRef, signal, viewChild } from '@angular/core';

export interface Confirmar {
  titulo: string;
  mensaje: string;
  accion: string;
  peligro?: boolean;
}

// Confirmación previa a cambios con efectos en cascada (desactivar servicios,
// módulos o cuentas).
@Component({
  selector: 'app-confirmacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog #dialogo class="dialogo" (close)="alCerrar()" aria-labelledby="confirmar-titulo">
      @if (datos(); as d) {
        <div class="dialogo__contenido">
          <h2 id="confirmar-titulo">{{ d.titulo }}</h2>
          <p>{{ d.mensaje }}</p>
          <div class="dialogo__pie">
            <button type="button" class="boton boton--secundario" (click)="cerrar(false)">Volver</button>
            <button
              type="button"
              [class]="d.peligro ? 'boton boton--peligro-solido' : 'boton boton--primario'"
              (click)="cerrar(true)"
            >
              {{ d.accion }}
            </button>
          </div>
        </div>
      }
    </dialog>
  `,
})
export class Confirmacion {
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  protected readonly datos = signal<Confirmar | null>(null);
  private resolver: ((valor: boolean) => void) | null = null;
  private resultado = false;

  abrir(datos: Confirmar): Promise<boolean> {
    this.datos.set(datos);
    this.resultado = false;
    this.dialogo().nativeElement.showModal();
    return new Promise((resolver) => (this.resolver = resolver));
  }

  protected cerrar(resultado: boolean): void {
    this.resultado = resultado;
    this.dialogo().nativeElement.close();
  }

  protected alCerrar(): void {
    this.resolver?.(this.resultado);
    this.resolver = null;
  }
}
