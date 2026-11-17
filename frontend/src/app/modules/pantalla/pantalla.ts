import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CodigoTurno } from '../../components/codigo-turno';
import { MarcaBanco } from '../../components/marca';
import { TurneroApi } from '../../services/api';
import { fechaLarga, hora, horaConSegundos } from '../../services/fechas';
import { PantallaPublica, TurnoPublicoCola } from '../../services/modelos';

const INTERVALO_MS = 5000;

interface ColaServicio {
  servicio: string;
  turnos: TurnoPublicoCola[];
}

// Pantalla de la sala de espera (sin sesión). Consulta GET /publico/pantalla
// cada 5 segundos; el DTO público no trae identificadorCliente ni turnos en
// estados terminales.
@Component({
  selector: 'app-pantalla',
  imports: [CodigoTurno, MarcaBanco],
  templateUrl: './pantalla.html',
  styleUrl: './pantalla.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pantalla implements OnInit {
  private readonly api = inject(TurneroApi);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly datos = signal<PantallaPublica | null>(null);
  protected readonly sinConexion = signal(false);
  protected readonly recienLlamado = signal<string | null>(null);
  protected readonly pantallaCompleta = signal(false);

  private conocidos: Set<string> | null = null;
  private temporizadorDestacado: ReturnType<typeof setTimeout> | null = null;

  protected readonly hora = computed(() => hora(this.datos()?.horaServidor));
  protected readonly actualizado = computed(() => horaConSegundos(this.datos()?.horaServidor));
  protected readonly dia = computed(() => fechaLarga(this.datos()?.fechaOperacion));

  // El backend ordena por fecha de llamado descendente: el primer LLAMADO es
  // el más reciente y es el que la sala debe ver primero.
  protected readonly destacado = computed(
    () => this.datos()?.turnosEnModulos.find((t) => t.estado === 'LLAMADO') ?? null,
  );
  protected readonly resto = computed(() => {
    const destacado = this.destacado();
    return (this.datos()?.turnosEnModulos ?? []).filter((t) => t !== destacado);
  });

  protected readonly colas = computed<ColaServicio[]>(() => {
    const grupos = new Map<string, TurnoPublicoCola[]>();
    for (const turno of this.datos()?.proximosEnEspera ?? []) {
      const lista = grupos.get(turno.servicioNombre) ?? [];
      lista.push(turno);
      grupos.set(turno.servicioNombre, lista);
    }
    return [...grupos].map(([servicio, turnos]) => ({
      servicio,
      turnos: [...turnos].sort((a, b) => a.ordenEnCola - b.ordenEnCola),
    }));
  });

  ngOnInit(): void {
    this.consultar();
    const intervalo = setInterval(() => this.consultar(), INTERVALO_MS);
    const alCambiarPantalla = () => this.pantallaCompleta.set(!!document.fullscreenElement);
    document.addEventListener('fullscreenchange', alCambiarPantalla);
    this.destroyRef.onDestroy(() => {
      clearInterval(intervalo);
      if (this.temporizadorDestacado) clearTimeout(this.temporizadorDestacado);
      document.removeEventListener('fullscreenchange', alCambiarPantalla);
    });
  }

  protected alternarPantallaCompleta(): void {
    if (document.fullscreenElement) {
      void document.exitFullscreen();
    } else {
      void document.documentElement.requestFullscreen?.();
    }
  }

  private consultar(): void {
    this.api.pantalla().subscribe({
      next: (datos) => {
        this.detectarLlamados(datos);
        this.datos.set(datos);
        this.sinConexion.set(false);
      },
      // Se conserva la última información y se reintenta en el siguiente ciclo
      error: () => this.sinConexion.set(true),
    });
  }

  // Resalta durante unos segundos un turno que acaba de pasar a LLAMADO.
  private detectarLlamados(datos: PantallaPublica): void {
    const llamados = datos.turnosEnModulos.filter((t) => t.estado === 'LLAMADO').map((t) => t.codigo);
    if (this.conocidos) {
      const nuevo = llamados.find((codigo) => !this.conocidos!.has(codigo));
      if (nuevo) {
        this.recienLlamado.set(nuevo);
        if (this.temporizadorDestacado) clearTimeout(this.temporizadorDestacado);
        this.temporizadorDestacado = setTimeout(() => this.recienLlamado.set(null), 8000);
      }
    }
    this.conocidos = new Set(llamados);
  }
}
