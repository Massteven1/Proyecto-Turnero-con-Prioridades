import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { TurneroApi } from '../services/api';
import { Avisos } from '../services/avisos';
import { leerError } from '../services/errores';
import { ETIQUETA_ESTADO, esActivo } from '../services/etiquetas';
import { fecha, fechaHora, hoyBogota, minutosEntre } from '../services/fechas';
import { EstadoDestino, Turno, TurnoDetalle as Detalle } from '../services/modelos';
import { SesionService } from '../services/sesion';
import { CodigoTurno } from './codigo-turno';
import { DialogoMotivo } from './dialogo-motivo';
import { ConjuntoBadge, EstadoTurnoBadge } from './estado-turno';

interface Accion {
  estado: EstadoDestino;
  texto: string;
  clase: string;
}

const ACCIONES: Record<string, Accion[]> = {
  EN_ESPERA: [{ estado: 'CANCELADO', texto: 'Cancelar turno', clase: 'boton boton--peligro' }],
  LLAMADO: [
    { estado: 'EN_ATENCION', texto: 'Iniciar atención', clase: 'boton boton--exito' },
    { estado: 'AUSENTE', texto: 'Marcar ausente', clase: 'boton boton--peligro' },
    { estado: 'CANCELADO', texto: 'Cancelar turno', clase: 'boton boton--peligro' },
  ],
  EN_ATENCION: [{ estado: 'FINALIZADO', texto: 'Finalizar atención', clase: 'boton boton--exito' }],
};

// Cajón lateral con el detalle de un turno (GET /turnos/{id}) y su historial
// cronológico de transiciones. Con [acciones]=true ofrece las transiciones que
// permite el estado actual y el rol de la sesión.
@Component({
  selector: 'app-turno-detalle',
  imports: [CodigoTurno, EstadoTurnoBadge, ConjuntoBadge, DialogoMotivo],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './turno-detalle.html',
  styleUrl: './turno-detalle.css',
})
export class TurnoDetalle {
  private readonly api = inject(TurneroApi);
  private readonly avisos = inject(Avisos);
  private readonly sesion = inject(SesionService);

  readonly acciones = input(false);
  readonly cambio = output<Turno>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private readonly motivo = viewChild.required(DialogoMotivo);

  protected readonly turno = signal<Detalle | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly enCurso = signal<EstadoDestino | null>(null);

  protected readonly fecha = fecha;
  protected readonly fechaHora = fechaHora;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO;

  protected readonly disponibles = computed<Accion[]>(() => {
    const t = this.turno();
    return t ? (ACCIONES[t.estado] ?? []) : [];
  });

  // Razón por la que la sesión no puede operar el turno, o null si puede.
  protected readonly bloqueo = computed<string | null>(() => {
    const t = this.turno();
    if (!t || this.sesion.esAdmin()) return null;
    if (t.estado === 'EN_ESPERA' && t.fechaOperacion !== hoyBogota()) {
      return 'Solo un administrador puede cancelar turnos de días anteriores.';
    }
    if (esActivo(t.estado) && t.operadorId !== this.sesion.sesion()?.usuarioId) {
      return `Este turno está asignado a ${t.operadorUsername ?? 'otro operador'}.`;
    }
    return null;
  });

  protected readonly espera = computed(() => {
    const t = this.turno();
    return t ? minutosEntre(t.fechaIngreso, t.fechaInicioAtencion) : null;
  });

  async abrir(id: number): Promise<void> {
    this.turno.set(null);
    this.error.set(null);
    if (!this.dialogo().nativeElement.open) this.dialogo().nativeElement.showModal();
    await this.cargar(id);
  }

  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  protected async ejecutar(accion: Accion): Promise<void> {
    const t = this.turno();
    if (!t) return;
    let motivo: string | null = null;
    if (accion.estado === 'CANCELADO' || accion.estado === 'AUSENTE') {
      motivo = await this.motivo().abrir(accion.estado, t.codigo);
      if (motivo === null) return;
    }
    this.enCurso.set(accion.estado);
    try {
      const actualizado = await firstValueFrom(
        this.api.cambiarEstado(t.id, { nuevoEstado: accion.estado, motivo }),
      );
      this.avisos.exito(`Turno ${t.codigo}: ${ETIQUETA_ESTADO[actualizado.estado].toLowerCase()}.`);
      this.cambio.emit(actualizado);
      await this.cargar(t.id);
    } catch (e) {
      this.avisos.error(leerError(e).mensaje);
    } finally {
      this.enCurso.set(null);
    }
  }

  private async cargar(id: number): Promise<void> {
    this.cargando.set(true);
    try {
      this.turno.set(await firstValueFrom(this.api.turno(id)));
    } catch (e) {
      this.error.set(leerError(e).mensaje);
    } finally {
      this.cargando.set(false);
    }
  }
}
