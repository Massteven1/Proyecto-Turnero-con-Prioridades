import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { TurneroApi } from '../../services/api';
import { Avisos } from '../../services/avisos';
import { leerError } from '../../services/errores';
import { Categoria } from '../../services/modelos';

// Categorías de prioridad (HU-04). Las cuatro categorías base no se eliminan,
// no se desactivan y no cambian su prioridad: solo admiten renombrarse.
@Component({
  selector: 'app-categorias',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="encabezado">
      <div>
        <h1>Categorías de prioridad</h1>
        <p>
          Discapacidad, Embarazada y Adulto mayor tienen el mismo nivel preferente; Regular no tiene prioridad. Solo
          puedes cambiar el nombre visible: los turnos ya emitidos conservan el nombre que tenía la categoría al
          emitirse.
        </p>
      </div>
    </header>

    <section class="area" aria-label="Categorías">
      @if (error(); as e) {
        <p class="nota nota--error" role="alert">{{ e }}</p>
      }
      @if (cargando()) {
        <p class="cargando">Cargando categorías…</p>
      } @else {
        <div class="tabla-contenedor">
          <table class="tabla">
            <thead>
              <tr>
                <th scope="col">Nombre visible</th>
                <th scope="col">Prioridad</th>
                <th scope="col">Estado</th>
                <th scope="col" class="acciones"><span class="solo-lector">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (c of categorias(); track c.id) {
                <tr>
                  <td>
                    <label class="solo-lector" [for]="'cat-' + c.id">Nombre de la categoría {{ c.nombre }}</label>
                    <input
                      class="entrada entrada--chica"
                      [id]="'cat-' + c.id"
                      [value]="borrador(c)"
                      maxlength="50"
                      (input)="editar(c.id, $any($event.target).value)"
                      (keydown.enter)="guardar(c)"
                    />
                    @if (errorNombre(c); as e) {
                      <span class="campo__error">{{ e }}</span>
                    }
                  </td>
                  <td>
                    <span [class]="c.preferente ? 'conjunto conjunto--preferente' : 'conjunto conjunto--regular'">
                      {{ c.preferente ? 'Preferente' : 'Sin prioridad' }}
                    </span>
                  </td>
                  <td><span class="estado estado--activo">Siempre activa</span></td>
                  <td class="acciones">
                    <button
                      type="button"
                      class="boton boton--secundario boton--chico"
                      [disabled]="!puedeGuardar(c) || guardando() === c.id"
                      (click)="guardar(c)"
                    >
                      {{ guardando() === c.id ? 'Guardando…' : 'Guardar nombre' }}
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <p class="texto-chico texto-tenue tabla-nota">La prioridad y el estado de las categorías base son fijos.</p>
      }
    </section>
  `,
})
export class Categorias implements OnInit {
  private readonly api = inject(TurneroApi);
  private readonly avisos = inject(Avisos);

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly borradores = signal<Record<number, string>>({});
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly guardando = signal<number | null>(null);

  ngOnInit(): void {
    void this.cargar();
  }

  protected borrador(c: Categoria): string {
    return this.borradores()[c.id] ?? c.nombre;
  }

  protected editar(id: number, valor: string): void {
    this.borradores.update((b) => ({ ...b, [id]: valor }));
  }

  protected errorNombre(c: Categoria): string | null {
    const valor = this.borrador(c).trim();
    if (valor === c.nombre) return null;
    if (valor.length < 3) return 'Escribe al menos 3 caracteres.';
    if (valor.length > 50) return 'Usa como máximo 50 caracteres.';
    return null;
  }

  protected puedeGuardar(c: Categoria): boolean {
    const valor = this.borrador(c).trim();
    return valor !== c.nombre && !this.errorNombre(c);
  }

  protected async guardar(c: Categoria): Promise<void> {
    if (!this.puedeGuardar(c)) return;
    const nombre = this.borrador(c).trim();
    this.guardando.set(c.id);
    try {
      const actualizada = await firstValueFrom(this.api.renombrarCategoria(c.id, nombre));
      this.categorias.update((lista) => lista.map((x) => (x.id === c.id ? actualizada : x)));
      this.borradores.update((b) => {
        const { [c.id]: _, ...resto } = b;
        return resto;
      });
      this.avisos.exito(`Categoría renombrada a ${actualizada.nombre}.`);
    } catch (e) {
      this.avisos.error(leerError(e).mensaje);
    } finally {
      this.guardando.set(null);
    }
  }

  private async cargar(): Promise<void> {
    try {
      this.categorias.set(await firstValueFrom(this.api.categorias()));
      this.error.set(null);
    } catch (e) {
      this.error.set(leerError(e).mensaje);
    } finally {
      this.cargando.set(false);
    }
  }
}
