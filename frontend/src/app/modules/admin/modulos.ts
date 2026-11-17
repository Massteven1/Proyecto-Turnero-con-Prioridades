import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CodigoTurno } from '../../components/codigo-turno';
import { Confirmacion } from '../../components/confirmacion';
import { ActivoBadge } from '../../components/estado-turno';
import { TurneroApi } from '../../services/api';
import { Avisos } from '../../services/avisos';
import { leerError } from '../../services/errores';
import { Modulo, Servicio } from '../../services/modelos';
import { mensajeCampo, texto } from '../../services/validadores';

// Módulos o ventanillas (HU-03). Un módulo ocupado no se puede desactivar y
// uno de un servicio inactivo no se puede activar: la interfaz lo deshabilita
// y el backend lo vuelve a validar (409).
@Component({
  selector: 'app-modulos',
  imports: [ReactiveFormsModule, ActivoBadge, CodigoTurno, Confirmacion],
  templateUrl: './modulos.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Modulos implements OnInit {
  private readonly api = inject(TurneroApi);
  private readonly avisos = inject(Avisos);
  private readonly confirmacion = viewChild.required(Confirmacion);

  protected readonly modulos = signal<Modulo[]>([]);
  protected readonly servicios = signal<Servicio[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly filtroServicio = signal<number>(0);
  protected readonly editando = signal<Modulo | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorForm = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    servicioId: [0, [Validators.min(1)]],
    nombre: ['', [texto(3, 50)]],
    activo: [true],
  });

  protected readonly visibles = computed(() => {
    const filtro = this.filtroServicio();
    return this.modulos().filter((m) => !filtro || m.servicioId === filtro);
  });

  protected readonly serviciosActivos = computed(() => this.servicios().filter((s) => s.activo));

  ngOnInit(): void {
    void this.cargar();
  }

  protected servicioActivo(servicioId: number): boolean {
    return this.servicios().find((s) => s.id === servicioId)?.activo ?? false;
  }

  protected elegirFiltro(valor: string): void {
    this.filtroServicio.set(Number(valor) || 0);
  }

  protected nuevo(): void {
    this.editando.set(null);
    this.errorForm.set(null);
    this.form.reset({
      servicioId: this.filtroServicio() || this.serviciosActivos()[0]?.id || 0,
      nombre: '',
      activo: true,
    });
  }

  protected editar(modulo: Modulo): void {
    this.editando.set(modulo);
    this.errorForm.set(null);
    this.form.reset({ servicioId: modulo.servicioId, nombre: modulo.nombre, activo: modulo.activo });
  }

  protected errorCampo(nombre: 'nombre' | 'servicioId'): string | null {
    const control = this.form.controls[nombre];
    if (nombre === 'servicioId' && control.errors?.['min'] && control.touched) return 'Elige un servicio.';
    return mensajeCampo(control);
  }

  protected async guardar(): Promise<void> {
    this.form.markAllAsTouched();
    this.errorForm.set(null);
    if (this.form.invalid || this.guardando()) return;
    const v = this.form.getRawValue();
    const actual = this.editando();
    if (actual && actual.activo && !v.activo && !(await this.confirmarBaja(actual))) return;

    this.guardando.set(true);
    try {
      if (actual) {
        await firstValueFrom(
          this.api.actualizarModulo(actual.id, { servicioId: v.servicioId, nombre: v.nombre.trim(), activo: v.activo }),
        );
        this.avisos.exito(`Módulo ${v.nombre.trim()} actualizado.`);
      } else {
        await firstValueFrom(this.api.crearModulo({ servicioId: v.servicioId, nombre: v.nombre.trim() }));
        this.avisos.exito(`Módulo ${v.nombre.trim()} creado y activo.`);
      }
      this.nuevo();
      await this.cargar();
    } catch (e) {
      this.errorForm.set(leerError(e).mensaje);
    } finally {
      this.guardando.set(false);
    }
  }

  protected async cambiarActivo(modulo: Modulo): Promise<void> {
    const activar = !modulo.activo;
    if (!activar && !(await this.confirmarBaja(modulo))) return;
    try {
      await firstValueFrom(
        this.api.actualizarModulo(modulo.id, { servicioId: modulo.servicioId, nombre: modulo.nombre, activo: activar }),
      );
      this.avisos.exito(`Módulo ${modulo.nombre} ${activar ? 'activado' : 'desactivado'}.`);
      if (this.editando()?.id === modulo.id) this.nuevo();
      await this.cargar();
    } catch (e) {
      this.avisos.error(leerError(e).mensaje);
    }
  }

  private confirmarBaja(modulo: Modulo): Promise<boolean> {
    return this.confirmacion().abrir({
      titulo: `Desactivar ${modulo.nombre}`,
      mensaje:
        'La ventanilla dejará de aparecer en el panel de los operadores. No se puede desactivar la última ventanilla activa de un servicio que tenga turnos en espera hoy.',
      accion: 'Desactivar módulo',
      peligro: true,
    });
  }

  private async cargar(): Promise<void> {
    try {
      const [modulos, servicios] = await Promise.all([
        firstValueFrom(this.api.modulos()),
        firstValueFrom(this.api.servicios()),
      ]);
      this.modulos.set(modulos);
      this.servicios.set(servicios);
      this.error.set(null);
      if (!this.editando() && !this.form.controls.servicioId.value) {
        this.form.controls.servicioId.setValue(this.serviciosActivos()[0]?.id ?? 0);
      }
    } catch (e) {
      this.error.set(leerError(e).mensaje);
    } finally {
      this.cargando.set(false);
    }
  }
}
