import { HttpClient, HttpParams } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from './api-url';
import {
  AuthRespuesta,
  CambioEstado,
  Categoria,
  EstadoCola,
  FiltroHistorial,
  Indicadores,
  LoginPeticion,
  Modulo,
  ModuloActualizar,
  ModuloCrear,
  PantallaPublica,
  ParametrosCola,
  RegistroTurno,
  Servicio,
  ServicioActualizar,
  ServicioCrear,
  SiguienteTurno,
  Turno,
  TurnoCola,
  TurnoDetalle,
  TurnoResumen,
  Usuario,
  UsuarioActualizar,
  UsuarioCrear,
} from './modelos';

// Cliente de los 21 endpoints del contrato. El token lo agrega authInterceptor.
@Service()
export class TurneroApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_URL);

  // Autenticación
  login(peticion: LoginPeticion): Observable<AuthRespuesta> {
    return this.http.post<AuthRespuesta>(`${this.base}/auth/login`, peticion);
  }

  // Usuarios (ADMIN)
  usuarios(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.base}/usuarios`);
  }

  crearUsuario(peticion: UsuarioCrear): Observable<Usuario> {
    return this.http.post<Usuario>(`${this.base}/usuarios`, peticion);
  }

  actualizarUsuario(id: number, peticion: UsuarioActualizar): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.base}/usuarios/${id}`, peticion);
  }

  // Servicios
  servicios(activo?: boolean): Observable<Servicio[]> {
    return this.http.get<Servicio[]>(`${this.base}/servicios`, { params: parametros({ activo }) });
  }

  crearServicio(peticion: ServicioCrear): Observable<Servicio> {
    return this.http.post<Servicio>(`${this.base}/servicios`, peticion);
  }

  actualizarServicio(id: number, peticion: ServicioActualizar): Observable<Servicio> {
    return this.http.put<Servicio>(`${this.base}/servicios/${id}`, peticion);
  }

  // Módulos de atención
  modulos(filtro: { servicioId?: number; activo?: boolean } = {}): Observable<Modulo[]> {
    return this.http.get<Modulo[]>(`${this.base}/modulos`, { params: parametros(filtro) });
  }

  crearModulo(peticion: ModuloCrear): Observable<Modulo> {
    return this.http.post<Modulo>(`${this.base}/modulos`, peticion);
  }

  actualizarModulo(id: number, peticion: ModuloActualizar): Observable<Modulo> {
    return this.http.put<Modulo>(`${this.base}/modulos/${id}`, peticion);
  }

  llamarSiguiente(moduloId: number): Observable<SiguienteTurno> {
    return this.http.post<SiguienteTurno>(`${this.base}/modulos/${moduloId}/siguiente`, null);
  }

  // Categorías
  categorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.base}/categorias`);
  }

  renombrarCategoria(id: number, nombre: string): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.base}/categorias/${id}`, { nombre });
  }

  // Parámetros de cola
  parametrosCola(): Observable<EstadoCola[]> {
    return this.http.get<EstadoCola[]>(`${this.base}/parametros-cola`);
  }

  actualizarParametros(servicioId: number, peticion: ParametrosCola): Observable<EstadoCola> {
    return this.http.put<EstadoCola>(`${this.base}/parametros-cola/${servicioId}`, peticion);
  }

  // Turnos
  registrarTurno(peticion: RegistroTurno): Observable<Turno> {
    return this.http.post<Turno>(`${this.base}/turnos`, peticion);
  }

  cambiarEstado(id: number, peticion: CambioEstado): Observable<Turno> {
    return this.http.patch<Turno>(`${this.base}/turnos/${id}/estado`, peticion);
  }

  cola(servicioId: number): Observable<TurnoCola[]> {
    return this.http.get<TurnoCola[]>(`${this.base}/turnos`, {
      params: parametros({ modo: 'COLA', servicioId }),
    });
  }

  historial(filtro: FiltroHistorial): Observable<TurnoResumen[]> {
    return this.http.get<TurnoResumen[]>(`${this.base}/turnos`, {
      params: parametros({ modo: 'HISTORIAL', ...filtro }),
    });
  }

  turno(id: number): Observable<TurnoDetalle> {
    return this.http.get<TurnoDetalle>(`${this.base}/turnos/${id}`);
  }

  indicadores(filtro: { fecha?: string | null; servicioId?: number | null }): Observable<Indicadores> {
    return this.http.get<Indicadores>(`${this.base}/turnos/indicadores`, {
      params: parametros(filtro),
    });
  }

  // Pantalla pública (sin JWT)
  pantalla(): Observable<PantallaPublica> {
    return this.http.get<PantallaPublica>(`${this.base}/publico/pantalla`);
  }
}

// Omite los filtros vacíos para no enviar parámetros que el backend rechaza
// (por ejemplo fecha o estado en modo COLA).
function parametros(valores: Record<string, string | number | boolean | null | undefined>): HttpParams {
  let params = new HttpParams();
  for (const [clave, valor] of Object.entries(valores)) {
    if (valor !== null && valor !== undefined && valor !== '') {
      params = params.set(clave, String(valor));
    }
  }
  return params;
}
