import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CodigoTurno } from '../../components/codigo-turno';
import { EstadoTurnoBadge } from '../../components/estado-turno';
import { TurnoDetalle } from '../../components/turno-detalle';
import { TurneroApi } from '../../services/api';
import { leerError } from '../../services/errores';
import { ESTADOS, ETIQUETA_ESTADO } from '../../services/etiquetas';
import { fecha, hora, hoyBogota } from '../../services/fechas';
import { EstadoTurno, Indicadores, Servicio, TurnoResumen } from '../../services/modelos';

interface Segmento {
  clave: string;
  etiqueta: string;
  valor: number;
}

// Historial e indicadores (HU-09, HU-10): auditoría de turnos por fecha,
// servicio y estado, indicadores de la jornada y transiciones de cada turno.
@Component({
  selector: 'app-historial',
  imports: [ReactiveFormsModule, CodigoTurno, EstadoTurnoBadge, TurnoDetalle],
  templateUrl: './historial.html',
  styleUrl: './historial.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Historial implements OnInit {
  private readonly api = inject(TurneroApi);
  private readonly detalle = viewChild.required(TurnoDetalle);

  protected readonly servicios = signal<Servicio[]>([]);
  protected readonly turnos = signal<TurnoResumen[]>([]);
  protected readonly indicadores = signal<Indicadores | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly seleccionado = signal<number | null>(null);
  protected readonly aplicados = signal<{ fecha: string; servicioId: number; estado: string }>({
    fecha: hoyBogota(),
    servicioId: 0,
    estado: '',
  });

  protected readonly estados = ESTADOS;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO;
  protected readonly fecha = fecha;
  protected readonly hora = hora;

  protected readonly filtros = inject(NonNullableFormBuilder).group({
    fecha: [hoyBogota()],
    servicioId: [0],
    estado: [''],
  });

  protected readonly segmentos = computed<Segmento[]>(() => {
    const i = this.indicadores();
    if (!i) return [];
    return [
      { clave: 'finalizado', etiqueta: 'Finalizados', valor: i.totalFinalizados },
      { clave: 'atencion', etiqueta: 'En atención o llamados', valor: i.totalEnAtencionOLlamado },
      { clave: 'espera', etiqueta: 'En espera', valor: i.totalEnEspera },
      { clave: 'cancelado', etiqueta: 'Cancelados', valor: i.totalCancelados },
      { clave: 'ausente', etiqueta: 'Ausentes', valor: i.totalAusentes },
    ];
  });

  protected readonly nombreServicio = computed(() => {
    const id = this.aplicados().servicioId;
    return id ? (this.servicios().find((s) => s.id === id)?.nombre ?? 'Servicio') : 'Todos los servicios';
  });

  async ngOnInit(): Promise<void> {
    try {
      this.servicios.set(await firstValueFrom(this.api.servicios()));
    } catch (e) {
      this.error.set(leerError(e).mensaje);
    }
    await this.consultar();
  }

  protected ancho(valor: number): number {
    const total = this.indicadores()?.totalEmitidos ?? 0;
    return total ? (valor / total) * 100 : 0;
  }

  protected async aplicar(): Promise<void> {
    await this.consultar();
  }

  protected async limpiar(): Promise<void> {
    this.filtros.reset({ fecha: hoyBogota(), servicioId: 0, estado: '' });
    await this.consultar();
  }

  protected ver(id: number): void {
    this.seleccionado.set(id);
    void this.detalle().abrir(id);
  }

  protected alCambiar(): void {
    void this.consultar();
  }

  private async consultar(): Promise<void> {
    const v = this.filtros.getRawValue();
    this.cargando.set(true);
    this.error.set(null);
    try {
      const [turnos, indicadores] = await Promise.all([
        firstValueFrom(
          this.api.historial({
            fecha: v.fecha || null,
            servicioId: v.servicioId || null,
            estado: (v.estado || null) as EstadoTurno | null,
          }),
        ),
        firstValueFrom(this.api.indicadores({ fecha: v.fecha || null, servicioId: v.servicioId || null })),
      ]);
      this.turnos.set(turnos);
      this.indicadores.set(indicadores);
      this.aplicados.set({ ...v });
    } catch (e) {
      this.error.set(leerError(e).mensaje);
    } finally {
      this.cargando.set(false);
    }
  }
}
