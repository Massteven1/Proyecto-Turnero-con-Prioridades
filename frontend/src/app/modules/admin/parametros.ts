import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { TurneroApi } from '../../services/api';
import { Avisos } from '../../services/avisos';
import { leerError } from '../../services/errores';
import { fecha } from '../../services/fechas';
import { EstadoCola, ParametrosCola } from '../../services/modelos';

interface Borrador {
  umbral: string;
}

// Parámetros de cola por servicio (HU-04): umbral de envejecimiento (1 a 240
@Component({
  selector: 'app-parametros',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="encabezado">
      <div>
        <h1>Parámetros de cola</h1>
        <p>
          Controlan cómo el sistema elige el siguiente turno de cada servicio. Los cambios aplican desde el próximo
          llamado.
        </p>
      </div>
    </header>

    <dl class="glosario">
      <div>
        <dt>Espera máxima de un regular</dt>
        <dd>
          Minutos tras los cuales un turno regular compite con los preferentes según su hora de llegada. Entre 1 y 240.
        </dd>
      </div>
      <div>
        <dt>Proporción preferentes a regular</dt>
        <dd>
          Fija en 3 a 1: tras tres preferentes seguidos se llama un regular, cuando hay de ambos en espera. No se
          configura.
        </dd>
      </div>
    </dl>

    <section class="area" aria-label="Parámetros por servicio">
      @if (error(); as e) {
        <p class="nota nota--error" role="alert">{{ e }}</p>
      }
      @if (cargando()) {
        <p class="cargando">Cargando parámetros…</p>
      } @else {
        <div class="tabla-contenedor">
          <table class="tabla">
            <thead>
              <tr>
                <th scope="col">Servicio</th>
                <th scope="col">Jornada actual</th>
                <th scope="col">Espera máxima (min)</th>
                <th scope="col">Proporción</th>
                <th scope="col" class="acciones"><span class="solo-lector">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (p of estados(); track p.servicioId) {
                <tr>
                  <td>
                    <strong>{{ p.servicioNombre }}</strong>
                    <span class="texto-tenue codigo"> {{ p.servicioPrefijo }}</span>
                  </td>
                  <td class="texto-chico">
                    {{ fecha(p.fechaOperacion) }}<br />
                    <span class="texto-tenue">
                      Último consecutivo {{ p.ultimoConsecutivo }}. Preferentes seguidos {{ p.contadorPreferentes }} de
                      {{ p.proporcionPreferente }}.
                    </span>
                  </td>
                  <td>
                    <label class="solo-lector" [for]="'umbral-' + p.servicioId">Espera máxima en minutos para {{ p.servicioNombre }}</label>
                    <input
                      class="entrada entrada--chica entrada--numero"
                      type="number"
                      min="1"
                      max="240"
                      [id]="'umbral-' + p.servicioId"
                      [value]="valor(p).umbral"
                      (input)="editar(p.servicioId, 'umbral', $any($event.target).value)"
                    />
                  </td>
                  <td class="sin-salto">{{ p.proporcionPreferente }} a 1 <span class="texto-tenue">(fija)</span></td>
                  <td class="acciones">
                    @if (errorFila(p); as e) {
                      <span class="campo__error">{{ e }}</span>
                    }
                    <button
                      type="button"
                      class="boton boton--primario boton--chico"
                      [disabled]="!cambio(p) || !!errorFila(p) || guardando() === p.servicioId"
                      (click)="guardar(p)"
                    >
                      {{ guardando() === p.servicioId ? 'Guardando…' : 'Guardar' }}
                    </button>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5" class="vacio">No hay servicios configurados.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
  styles: `
    .glosario {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr));
      gap: 1rem 2rem;
      max-width: 60rem;
      margin: 0 0 1.25rem;
    }
    .glosario dt { font-weight: 700; }
    .glosario dd { margin: 0.2rem 0 0; color: var(--tinta-suave); font-size: var(--t-s); }
    .sin-salto { white-space: nowrap; }
    .acciones .campo__error { display: block; margin-bottom: 0.25rem; }
  `,
})
export class Parametros implements OnInit {
  private readonly api = inject(TurneroApi);
  private readonly avisos = inject(Avisos);

  protected readonly estados = signal<EstadoCola[]>([]);
  protected readonly borradores = signal<Record<number, Borrador>>({});
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly guardando = signal<number | null>(null);
  protected readonly fecha = fecha;

  ngOnInit(): void {
    void this.cargar();
  }

  protected valor(p: EstadoCola): Borrador {
    return (
      this.borradores()[p.servicioId] ?? {
        umbral: String(p.umbralEnvejecimientoMin),
      }
    );
  }

  protected editar(servicioId: number, campo: keyof Borrador, texto: string): void {
    const p = this.estados().find((e) => e.servicioId === servicioId);
    if (!p) return;
    this.borradores.update((b) => ({ ...b, [servicioId]: { ...this.valor(p), [campo]: texto } }));
  }

  protected errorFila(p: EstadoCola): string | null {
    const umbral = this.leer(p);
    if (!Number.isInteger(umbral) || umbral < 1 || umbral > 240) return 'Espera entre 1 y 240 minutos.';
    return null;
  }

  protected cambio(p: EstadoCola): boolean {
    return this.leer(p) !== p.umbralEnvejecimientoMin;
  }

  protected async guardar(p: EstadoCola): Promise<void> {
    if (!this.cambio(p) || this.errorFila(p)) return;
    const peticion: ParametrosCola = { umbralEnvejecimientoMin: this.leer(p) };
    this.guardando.set(p.servicioId);
    try {
      const actualizado = await firstValueFrom(this.api.actualizarParametros(p.servicioId, peticion));
      this.estados.update((lista) => lista.map((e) => (e.servicioId === p.servicioId ? actualizado : e)));
      this.borradores.update((b) => {
        const { [p.servicioId]: _, ...resto } = b;
        return resto;
      });
      this.avisos.exito(
        `${actualizado.servicioNombre}: espera máxima ${actualizado.umbralEnvejecimientoMin} min y proporción ${actualizado.proporcionPreferente} a 1.`,
      );
    } catch (e) {
      this.avisos.error(leerError(e).mensaje);
    } finally {
      this.guardando.set(null);
    }
  }

  private leer(p: EstadoCola): number {
    return Number(this.valor(p).umbral);
  }

  private async cargar(): Promise<void> {
    try {
      this.estados.set(await firstValueFrom(this.api.parametrosCola()));
      this.error.set(null);
    } catch (e) {
      this.error.set(leerError(e).mensaje);
    } finally {
      this.cargando.set(false);
    }
  }
}
