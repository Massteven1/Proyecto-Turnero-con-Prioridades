import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CodigoTurno } from '../../components/codigo-turno';
import { DialogoMotivo } from '../../components/dialogo-motivo';
import { ConjuntoBadge, EstadoTurnoBadge } from '../../components/estado-turno';
import { TurnoDetalle } from '../../components/turno-detalle';
import { TurneroApi } from '../../services/api';
import { Avisos } from '../../services/avisos';
import { leerError } from '../../services/errores';
import { ETIQUETA_ESTADO, esActivo, esTerminal } from '../../services/etiquetas';
import { hora, horaActualBogota } from '../../services/fechas';
import {
  Categoria,
  EstadoCola,
  EstadoDestino,
  Modulo,
  Servicio,
  Turno,
  TurnoCola,
} from '../../services/modelos';
import { SesionService } from '../../services/sesion';
import { mensajeCampo, texto } from '../../services/validadores';

const REFRESCO_MS = 10000;

interface GrupoModulos {
  servicio: string;
  modulos: Modulo[];
}

// Panel de ventanilla (HU-05, HU-06, HU-07): registrar turnos, llamar el
// siguiente con un solo botón, ejecutar las transiciones y ver la cola
// proyectada del servicio de la ventanilla elegida.
@Component({
  selector: 'app-operador',
  imports: [ReactiveFormsModule, CodigoTurno, EstadoTurnoBadge, ConjuntoBadge, DialogoMotivo, TurnoDetalle],
  templateUrl: './operador.html',
  styleUrl: './operador.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Operador implements OnInit {
  private readonly api = inject(TurneroApi);
  private readonly avisos = inject(Avisos);
  private readonly sesion = inject(SesionService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly motivo = viewChild.required(DialogoMotivo);
  private readonly detalle = viewChild.required(TurnoDetalle);

  protected readonly servicios = signal<Servicio[]>([]);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly modulos = signal<Modulo[]>([]);
  protected readonly parametros = signal<EstadoCola[]>([]);
  protected readonly cola = signal<TurnoCola[]>([]);
  protected readonly moduloId = signal<number | null>(null);
  protected readonly turno = signal<Turno | null>(null);
  protected readonly emitido = signal<Turno | null>(null);

  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<string | null>(null);
  protected readonly errorCola = signal<string | null>(null);
  protected readonly colaActualizada = signal<string | null>(null);
  protected readonly llamando = signal(false);
  protected readonly cambiando = signal<EstadoDestino | null>(null);
  protected readonly registrando = signal(false);
  protected readonly errorRegistro = signal<string | null>(null);

  protected readonly hora = hora;

  protected readonly form = inject(NonNullableFormBuilder).group({
    identificadorCliente: ['', [texto(3, 100)]],
    servicioId: [0, [Validators.min(1)]],
    categoriaId: [0, [Validators.min(1)]],
  });

  protected readonly serviciosActivos = computed(() => this.servicios().filter((s) => s.activo));
  protected readonly modulosActivos = computed(() => this.modulos().filter((m) => m.activo));

  protected readonly grupos = computed<GrupoModulos[]>(() => {
    const grupos = new Map<string, Modulo[]>();
    for (const m of this.modulosActivos()) {
      grupos.set(m.servicioNombre, [...(grupos.get(m.servicioNombre) ?? []), m]);
    }
    return [...grupos].map(([servicio, modulos]) => ({ servicio, modulos }));
  });

  protected readonly modulo = computed(
    () => this.modulosActivos().find((m) => m.id === this.moduloId()) ?? null,
  );

  protected readonly estadoCola = computed(
    () => this.parametros().find((p) => p.servicioId === this.modulo()?.servicioId) ?? null,
  );

  protected readonly turnoActivo = computed(() => {
    const t = this.turno();
    return t && esActivo(t.estado) ? t : null;
  });

  protected readonly turnoCerrado = computed(() => {
    const t = this.turno();
    return t && esTerminal(t.estado) ? t : null;
  });

  // Por qué no se puede llamar, o null si el botón está disponible.
  protected readonly razonNoLlamar = computed<string | null>(() => {
    const m = this.modulo();
    if (!m) return 'Elige tu ventanilla para llamar turnos.';
    const activo = this.turnoActivo();
    if (activo) return `Resuelve el turno ${activo.codigo} antes de llamar al siguiente.`;
    if (m.ocupado) return `${m.nombre} está atendiendo el turno ${m.turnoActivoCodigo}.`;
    return null;
  });

  protected readonly pips = computed(() => {
    const p = this.estadoCola();
    if (!p) return [];
    return Array.from({ length: p.proporcionPreferente }, (_, i) => i < p.contadorPreferentes);
  });

  protected readonly tocaRegular = computed(() => {
    const p = this.estadoCola();
    return !!p && p.contadorPreferentes >= p.proporcionPreferente;
  });

  // Recorrido del turno en la ventanilla: llamado, atención y cierre.
  protected readonly pasos = computed(() => {
    const t = this.turno();
    if (!t) return [];
    const salida = (t.estado === 'CANCELADO' || t.estado === 'AUSENTE') && !t.fechaInicioAtencion;
    const pasos = [{ nombre: 'Llamado', hora: t.fechaLlamado, hecho: true, salida: false }];
    if (salida) {
      pasos.push({
        nombre: ETIQUETA_ESTADO[t.estado],
        hora: t.fechaCancelacionAusencia,
        hecho: true,
        salida: true,
      });
    } else {
      pasos.push(
        { nombre: 'En atención', hora: t.fechaInicioAtencion, hecho: !!t.fechaInicioAtencion, salida: false },
        { nombre: 'Finalizado', hora: t.fechaFinAtencion, hecho: !!t.fechaFinAtencion, salida: false },
      );
    }
    return pasos;
  });

  protected readonly etiquetaEstado = ETIQUETA_ESTADO;

  protected readonly categoriaRegularId = computed(
    () => this.categorias().find((c) => !c.preferente)?.id ?? 0,
  );

  async ngOnInit(): Promise<void> {
    try {
      const [servicios, categorias, modulos, parametros] = await Promise.all([
        firstValueFrom(this.api.servicios()),
        firstValueFrom(this.api.categorias()),
        firstValueFrom(this.api.modulos()),
        firstValueFrom(this.api.parametrosCola()),
      ]);
      this.servicios.set(servicios);
      this.categorias.set(categorias);
      this.modulos.set(modulos);
      this.parametros.set(parametros);

      await this.recuperarTurnoPropio(modulos);
      if (this.moduloId() === null) {
        this.moduloId.set(this.moduloGuardado(modulos));
      }
      this.form.patchValue({
        servicioId: this.modulo()?.servicioId ?? this.serviciosActivos()[0]?.id ?? 0,
        categoriaId: this.categoriaRegularId(),
      });
      await this.refrescar();
    } catch (e) {
      this.errorCarga.set(leerError(e).mensaje);
    } finally {
      this.cargando.set(false);
    }

    // La cola la comparten varias ventanillas: se refresca mientras la
    // pestaña está visible.
    const intervalo = setInterval(() => {
      if (document.visibilityState === 'visible') void this.refrescar();
    }, REFRESCO_MS);
    this.destroyRef.onDestroy(() => clearInterval(intervalo));
  }

  protected elegirModulo(valor: string): void {
    const id = Number(valor) || null;
    this.moduloId.set(id);
    const usuario = this.sesion.sesion()?.usuarioId;
    if (id && usuario) localStorage.setItem(`turnero.modulo.${usuario}`, String(id));
    const m = this.modulo();
    if (m) this.form.controls.servicioId.setValue(m.servicioId);
    this.cola.set([]);
    if (this.turnoCerrado()) this.turno.set(null);
    void this.refrescar();
  }

  protected async llamar(): Promise<void> {
    const m = this.modulo();
    if (!m || this.razonNoLlamar() || this.llamando()) return;
    this.llamando.set(true);
    try {
      const r = await firstValueFrom(this.api.llamarSiguiente(m.id));
      this.parametros.update((lista) =>
        lista.map((p) =>
          p.servicioId === m.servicioId ? { ...p, contadorPreferentes: r.contadorPreferentesActual } : p,
        ),
      );
      if (r.colaVacia || !r.turno) {
        this.avisos.info(r.mensaje || `No hay turnos en espera para ${m.servicioNombre}.`);
      } else {
        this.turno.set(r.turno);
        this.marcarOcupacion(m.id, r.turno);
        this.avisos.exito(`Turno ${r.turno.codigo} llamado a ${m.nombre}.`);
      }
    } catch (e) {
      this.avisos.error(leerError(e).mensaje);
    } finally {
      this.llamando.set(false);
      void this.refrescar();
    }
  }

  protected async cambiar(nuevo: EstadoDestino): Promise<void> {
    const t = this.turnoActivo();
    if (!t || this.cambiando()) return;
    let motivo: string | null = null;
    if (nuevo === 'CANCELADO' || nuevo === 'AUSENTE') {
      motivo = await this.motivo().abrir(nuevo, t.codigo);
      if (motivo === null) return;
    }
    this.cambiando.set(nuevo);
    try {
      const actualizado = await firstValueFrom(this.api.cambiarEstado(t.id, { nuevoEstado: nuevo, motivo }));
      this.turno.set(actualizado);
      this.avisos.exito(MENSAJE_CAMBIO[nuevo](actualizado.codigo));
      if (esTerminal(actualizado.estado)) {
        // La salida terminal libera la ventanilla: se refleja sin esperar al refresco
        this.marcarOcupacion(actualizado.moduloId, null);
        void this.refrescar();
      }
    } catch (e) {
      this.avisos.error(leerError(e).mensaje);
    } finally {
      this.cambiando.set(null);
    }
  }

  protected async cancelarEnCola(t: TurnoCola): Promise<void> {
    const motivo = await this.motivo().abrir('CANCELADO', t.codigo);
    if (motivo === null) return;
    try {
      await firstValueFrom(this.api.cambiarEstado(t.id, { nuevoEstado: 'CANCELADO', motivo }));
      this.avisos.exito(`Turno ${t.codigo} cancelado.`);
    } catch (e) {
      this.avisos.error(leerError(e).mensaje);
    }
    void this.refrescar();
  }

  protected verDetalle(id: number): void {
    void this.detalle().abrir(id);
  }

  protected alCambiarDesdeDetalle(): void {
    void this.refrescar();
  }

  protected async registrar(): Promise<void> {
    this.form.markAllAsTouched();
    this.errorRegistro.set(null);
    if (this.form.invalid || this.registrando()) return;
    const valor = this.form.getRawValue();
    this.registrando.set(true);
    try {
      const turno = await firstValueFrom(
        this.api.registrarTurno({
          identificadorCliente: valor.identificadorCliente.trim(),
          servicioId: valor.servicioId,
          categoriaId: valor.categoriaId,
        }),
      );
      this.emitido.set(turno);
      this.avisos.exito(`Turno ${turno.codigo} registrado.`);
      this.form.reset({
        identificadorCliente: '',
        servicioId: valor.servicioId,
        categoriaId: this.categoriaRegularId(),
      });
      if (turno.servicioId === this.modulo()?.servicioId) void this.refrescar();
    } catch (e) {
      const err = leerError(e);
      const campos = Object.values(err.campos);
      this.errorRegistro.set(campos.length ? `${err.mensaje} ${campos.join(' ')}` : err.mensaje);
    } finally {
      this.registrando.set(false);
    }
  }

  protected errorCampo(nombre: 'identificadorCliente'): string | null {
    return mensajeCampo(this.form.controls[nombre]);
  }

  protected async refrescar(): Promise<void> {
    const servicioId = this.modulo()?.servicioId ?? null;
    try {
      const [modulos, parametros, cola] = await Promise.all([
        firstValueFrom(this.api.modulos()),
        firstValueFrom(this.api.parametrosCola()),
        servicioId ? firstValueFrom(this.api.cola(servicioId)) : Promise.resolve([]),
      ]);
      this.modulos.set(modulos);
      this.parametros.set(parametros);
      if (servicioId === (this.modulo()?.servicioId ?? null)) this.cola.set(cola);
      this.errorCola.set(null);
      this.colaActualizada.set(horaActualBogota());
    } catch (e) {
      this.errorCola.set(leerError(e).mensaje);
    }
  }

  // Al entrar, si la cuenta ya tiene un turno LLAMADO o EN_ATENCION (por
  // ejemplo tras recargar), se retoma en su ventanilla.
  private async recuperarTurnoPropio(modulos: Modulo[]): Promise<void> {
    const yo = this.sesion.sesion()?.usuarioId;
    const ocupados = modulos.filter((m) => m.activo && m.turnoActivoId);
    const detalles = await Promise.all(
      ocupados.map((m) => firstValueFrom(this.api.turno(m.turnoActivoId!)).catch(() => null)),
    );
    const propio = detalles.find((d) => d && d.operadorId === yo && esActivo(d.estado));
    if (propio) {
      this.moduloId.set(propio.moduloId);
      this.turno.set(propio);
    }
  }

  private marcarOcupacion(moduloId: number | null, turno: Turno | null): void {
    this.modulos.update((lista) =>
      lista.map((m) =>
        m.id === moduloId
          ? { ...m, ocupado: !!turno, turnoActivoId: turno?.id ?? null, turnoActivoCodigo: turno?.codigo ?? null }
          : m,
      ),
    );
  }

  private moduloGuardado(modulos: Modulo[]): number | null {
    const usuario = this.sesion.sesion()?.usuarioId;
    const guardado = Number(localStorage.getItem(`turnero.modulo.${usuario}`));
    const activos = modulos.filter((m) => m.activo);
    if (activos.some((m) => m.id === guardado)) return guardado;
    return activos.length === 1 ? activos[0].id : null;
  }
}

const MENSAJE_CAMBIO: Record<EstadoDestino, (codigo: string) => string> = {
  EN_ATENCION: (codigo) => `Atención iniciada para el turno ${codigo}.`,
  FINALIZADO: (codigo) => `Atención del turno ${codigo} finalizada. La ventanilla quedó libre.`,
  CANCELADO: (codigo) => `Turno ${codigo} cancelado. La ventanilla quedó libre.`,
  AUSENTE: (codigo) => `Turno ${codigo} marcado como ausente. La ventanilla quedó libre.`,
};
