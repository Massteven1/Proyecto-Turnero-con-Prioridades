import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  signal,
  viewChild,
} from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { mensajeCampo, texto } from '../services/validadores';

export type AccionConMotivo = 'CANCELADO' | 'AUSENTE';

const SUGERENCIAS: Record<AccionConMotivo, string[]> = {
  AUSENTE: ['Cliente no se presentó en ventanilla', 'Cliente no respondió a dos llamados'],
  CANCELADO: [
    'Cliente desistió del trámite',
    'Cliente se retiró de la sucursal',
    'Turno registrado por error',
  ],
};

// Pide el motivo obligatorio (5 a 255 caracteres) para cancelar o marcar
// ausente. Devuelve el motivo o null si se cierra sin confirmar.
@Component({
  selector: 'app-dialogo-motivo',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog #dialogo class="dialogo" (close)="alCerrar()" aria-labelledby="motivo-titulo">
      <form class="dialogo__contenido" (submit)="confirmar($event)">
        <h2 id="motivo-titulo">{{ titulo() }}</h2>
        <p class="texto-tenue">
          {{ explicacion() }}
        </p>
        <div class="campo">
          <label class="campo__etiqueta" for="motivo-texto">Motivo</label>
          <textarea id="motivo-texto" class="entrada" [formControl]="motivo" maxlength="255" rows="3"></textarea>
          <span class="campo__ayuda">Entre 5 y 255 caracteres. Queda en el historial del turno.</span>
          @if (error(); as e) {
            <span class="campo__error" role="alert">{{ e }}</span>
          }
        </div>
        <div class="sugerencias" role="group" aria-label="Motivos frecuentes">
          @for (s of sugerencias(); track s) {
            <button type="button" class="boton boton--secundario boton--chico" (click)="usar(s)">{{ s }}</button>
          }
        </div>
        <div class="dialogo__pie">
          <button type="button" class="boton boton--secundario" (click)="dialogo.close()">Volver</button>
          <button type="submit" class="boton boton--peligro-solido">{{ textoAccion() }}</button>
        </div>
      </form>
    </dialog>
  `,
  styles: `
    .sugerencias { display: flex; flex-wrap: wrap; gap: 0.4rem; }
  `,
})
export class DialogoMotivo {
  protected readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  protected readonly motivo = new FormControl('', { nonNullable: true, validators: [texto(5, 255)] });
  private readonly accion = signal<AccionConMotivo>('CANCELADO');
  private readonly codigo = signal('');
  private resolver: ((valor: string | null) => void) | null = null;
  private confirmado: string | null = null;

  protected readonly titulo = computed(() =>
    this.accion() === 'AUSENTE' ? `Marcar ausente el turno ${this.codigo()}` : `Cancelar el turno ${this.codigo()}`,
  );
  protected readonly textoAccion = computed(() =>
    this.accion() === 'AUSENTE' ? 'Marcar ausente' : 'Cancelar turno',
  );
  protected readonly explicacion = computed(() =>
    this.accion() === 'AUSENTE'
      ? 'El turno sale de la ventanilla y no vuelve a la cola. La ventanilla queda libre para el siguiente llamado.'
      : 'El turno queda cerrado y no vuelve a la cola. Esta acción no se puede deshacer.',
  );
  protected readonly sugerencias = computed(() => SUGERENCIAS[this.accion()]);

  // Método y no computed: el estado del control no es una señal y debe
  // reevaluarse en cada detección de cambios.
  protected error(): string | null {
    return mensajeCampo(this.motivo);
  }

  abrir(accion: AccionConMotivo, codigo: string): Promise<string | null> {
    this.accion.set(accion);
    this.codigo.set(codigo);
    this.motivo.reset('');
    this.confirmado = null;
    this.dialogo().nativeElement.showModal();
    return new Promise((resolver) => (this.resolver = resolver));
  }

  protected usar(sugerencia: string): void {
    this.motivo.setValue(sugerencia);
    this.motivo.markAsDirty();
  }

  protected confirmar(evento: Event): void {
    evento.preventDefault();
    this.motivo.markAsTouched();
    if (this.motivo.invalid) return;
    this.confirmado = this.motivo.value.trim();
    this.dialogo().nativeElement.close();
  }

  protected alCerrar(): void {
    this.resolver?.(this.confirmado);
    this.resolver = null;
  }
}
