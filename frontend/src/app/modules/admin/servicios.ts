import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Confirmacion } from '../../components/confirmacion';
import { ActivoBadge } from '../../components/estado-turno';
import { TurneroApi } from '../../services/api';
import { Avisos } from '../../services/avisos';
import { leerError } from '../../services/errores';
import { Modulo, Servicio } from '../../services/modelos';
import { mensajeCampo, texto } from '../../services/validadores';

type Filtro = 'todos' | 'activos' | 'inactivos';

// Servicios bancarios (HU-03): alta, edición y baja lógica. El prefijo es
// inmutable porque forma parte de los códigos históricos de los turnos.
@Component({
  selector: 'app-servicios',
  imports: [ReactiveFormsModule, ActivoBadge, Confirmacion],
  templateUrl: './servicios.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Servicios implements OnInit {
  private readonly api = inject(TurneroApi);
  private readonly avisos = inject(Avisos);
  private readonly confirmacion = viewChild.required(Confirmacion);

  protected readonly servicios = signal<Servicio[]>([]);
  protected readonly modulos = signal<Modulo[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly filtro = signal<Filtro>('todos');
  protected readonly editando = signal<Servicio | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorForm = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    codigoPrefijo: ['', [Validators.required, Validators.pattern(/^[A-Z]{2,10}$/)]],
    nombre: ['', [texto(3, 100)]],
    descripcion: ['', [Validators.maxLength(255)]],
    activo: [true],
  });

  protected readonly visibles = computed(() => {
    const filtro = this.filtro();
    return this.servicios().filter((s) =>
      filtro === 'todos' ? true : filtro === 'activos' ? s.activo : !s.activo,
    );
  });

  ngOnInit(): void {
    void this.cargar();
  }

  protected modulosActivos(servicioId: number): number {
    return this.modulos().filter((m) => m.servicioId === servicioId && m.activo).length;
  }

  protected modulosTotales(servicioId: number): number {
    return this.modulos().filter((m) => m.servicioId === servicioId).length;
  }

  protected nuevo(): void {
    this.editando.set(null);
    this.errorForm.set(null);
    this.form.reset({ codigoPrefijo: '', nombre: '', descripcion: '', activo: true });
    this.form.controls.codigoPrefijo.enable();
  }

  protected editar(servicio: Servicio): void {
    this.editando.set(servicio);
    this.errorForm.set(null);
    this.form.reset({
      codigoPrefijo: servicio.codigoPrefijo,
      nombre: servicio.nombre,
      descripcion: servicio.descripcion ?? '',
      activo: servicio.activo,
    });
    this.form.controls.codigoPrefijo.disable();
  }

  protected mayusculas(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    this.form.controls.codigoPrefijo.setValue(entrada.value.toUpperCase().replace(/[^A-Z]/g, ''));
  }

  protected errorCampo(nombre: 'codigoPrefijo' | 'nombre' | 'descripcion'): string | null {
    const control = this.form.controls[nombre];
    if (nombre === 'codigoPrefijo' && control.errors?.['pattern'] && control.touched) {
      return 'Usa de 2 a 10 letras mayúsculas, sin tildes ni números.';
    }
    return mensajeCampo(control);
  }

  protected async guardar(): Promise<void> {
    this.form.markAllAsTouched();
    this.errorForm.set(null);
    if (this.form.invalid || this.guardando()) return;
    const v = this.form.getRawValue();
    const descripcion = v.descripcion.trim() || null;
    const actual = this.editando();
    if (actual && actual.activo && !v.activo && !(await this.confirmarBaja(actual))) return;

    this.guardando.set(true);
    try {
      if (actual) {
        await firstValueFrom(
          this.api.actualizarServicio(actual.id, { nombre: v.nombre.trim(), descripcion, activo: v.activo }),
        );
        this.avisos.exito(`Servicio ${v.nombre.trim()} actualizado.`);
      } else {
        await firstValueFrom(
          this.api.crearServicio({ codigoPrefijo: v.codigoPrefijo, nombre: v.nombre.trim(), descripcion }),
        );
        this.avisos.exito(`Servicio ${v.nombre.trim()} creado con parámetros de cola por defecto (15 min y 3 a 1).`);
      }
      this.nuevo();
      await this.cargar();
    } catch (e) {
      this.errorForm.set(leerError(e).mensaje);
    } finally {
      this.guardando.set(false);
    }
  }

  protected async cambiarActivo(servicio: Servicio): Promise<void> {
    const activar = !servicio.activo;
    if (!activar && !(await this.confirmarBaja(servicio))) return;
    try {
      await firstValueFrom(
        this.api.actualizarServicio(servicio.id, {
          nombre: servicio.nombre,
          descripcion: servicio.descripcion,
          activo: activar,
        }),
      );
      this.avisos.exito(
        activar
          ? `Servicio ${servicio.nombre} activado. Sus módulos siguen inactivos: actívalos desde Módulos de atención.`
          : `Servicio ${servicio.nombre} desactivado junto con sus módulos.`,
      );
      if (this.editando()?.id === servicio.id) this.nuevo();
      await this.cargar();
    } catch (e) {
      this.avisos.error(leerError(e).mensaje);
    }
  }

  private confirmarBaja(servicio: Servicio): Promise<boolean> {
    const activos = this.modulosActivos(servicio.id);
    return this.confirmacion().abrir({
      titulo: `Desactivar ${servicio.nombre}`,
      mensaje:
        `${activos === 1 ? 'Su módulo activo también se desactivará' : `Sus ${activos} módulos activos también se desactivarán`}. ` +
        'Si lo reactivas, tendrás que activar los módulos uno por uno. No se puede desactivar mientras tenga turnos en espera hoy o turnos en atención.',
      accion: 'Desactivar servicio',
      peligro: true,
    });
  }

  private async cargar(): Promise<void> {
    try {
      const [servicios, modulos] = await Promise.all([
        firstValueFrom(this.api.servicios()),
        firstValueFrom(this.api.modulos()),
      ]);
      this.servicios.set(servicios);
      this.modulos.set(modulos);
      this.error.set(null);
    } catch (e) {
      this.error.set(leerError(e).mensaje);
    } finally {
      this.cargando.set(false);
    }
  }
}
