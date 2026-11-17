import { Service, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { instanteBogota } from './fechas';
import { AuthRespuesta, Rol } from './modelos';

export interface Sesion {
  token: string;
  usuarioId: number;
  username: string;
  nombreCompleto: string;
  rol: Rol;
  expiracion: string;
}

export type MotivoCierre = 'expirada' | 'invalida';

// El JWT vive en sessionStorage: dura lo que la pestaña y nunca queda guardado
// en el equipo. "Recordar usuario" guarda solo el username en localStorage.
const CLAVE_SESION = 'turnero.sesion';
const CLAVE_USUARIO = 'turnero.usuario-recordado';
const MAX_TEMPORIZADOR = 2_147_000_000;

@Service()
export class SesionService {
  private readonly router = inject(Router);
  private readonly estado = signal<Sesion | null>(leerSesion());
  private temporizador: ReturnType<typeof setTimeout> | null = null;

  readonly sesion = this.estado.asReadonly();
  readonly rol = computed(() => this.estado()?.rol ?? null);
  readonly esAdmin = computed(() => this.estado()?.rol === 'ADMIN');

  constructor() {
    const actual = this.estado();
    if (actual && vencida(actual)) {
      this.limpiar();
    } else if (actual) {
      this.programarVencimiento(actual);
    }
  }

  iniciar(respuesta: AuthRespuesta): void {
    const sesion: Sesion = {
      token: respuesta.token,
      usuarioId: respuesta.usuarioId,
      username: respuesta.username,
      nombreCompleto: respuesta.nombreCompleto,
      rol: respuesta.rol,
      expiracion: respuesta.expiracion,
    };
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    this.estado.set(sesion);
    this.programarVencimiento(sesion);
  }

  // Token vigente o null. No navega: el vencimiento lo maneja el temporizador.
  token(): string | null {
    const actual = this.estado();
    return actual && !vencida(actual) ? actual.token : null;
  }

  cerrar(): void {
    this.limpiar();
    void this.router.navigate(['/login']);
  }

  // Cierre forzado: token vencido o rechazado por el backend con 401.
  terminar(motivo: MotivoCierre): void {
    if (!this.estado()) return;
    const volver = this.router.url.startsWith('/login') ? null : this.router.url;
    this.limpiar();
    void this.router.navigate(['/login'], {
      queryParams: { sesion: motivo, ...(volver ? { volver } : {}) },
    });
  }

  rutaInicial(rol: Rol | null = this.rol()): string {
    if (rol === 'ADMIN') return '/admin';
    if (rol === 'OPERADOR') return '/operacion';
    return '/login';
  }

  usuarioRecordado(): string {
    return localStorage.getItem(CLAVE_USUARIO) ?? '';
  }

  recordarUsuario(username: string | null): void {
    if (username) {
      localStorage.setItem(CLAVE_USUARIO, username);
    } else {
      localStorage.removeItem(CLAVE_USUARIO);
    }
  }

  private programarVencimiento(sesion: Sesion): void {
    if (this.temporizador) clearTimeout(this.temporizador);
    const restante = instanteBogota(sesion.expiracion) - Date.now();
    this.temporizador = setTimeout(
      () => this.terminar('expirada'),
      Math.min(Math.max(restante, 0), MAX_TEMPORIZADOR),
    );
  }

  private limpiar(): void {
    if (this.temporizador) clearTimeout(this.temporizador);
    this.temporizador = null;
    sessionStorage.removeItem(CLAVE_SESION);
    this.estado.set(null);
  }
}

function leerSesion(): Sesion | null {
  try {
    const texto = sessionStorage.getItem(CLAVE_SESION);
    return texto ? (JSON.parse(texto) as Sesion) : null;
  } catch {
    return null;
  }
}

function vencida(sesion: Sesion): boolean {
  const expira = instanteBogota(sesion.expiracion);
  return Number.isNaN(expira) || expira <= Date.now();
}
