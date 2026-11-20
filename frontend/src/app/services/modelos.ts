// Contratos de la API REST (/api/v1). Reflejan los DTO del backend; las fechas
// llegan como texto ISO sin zona (hora de America/Bogota).

export type Rol = 'ADMIN' | 'OPERADOR';

export type EstadoTurno =
  | 'EN_ESPERA'
  | 'LLAMADO'
  | 'EN_ATENCION'
  | 'FINALIZADO'
  | 'CANCELADO'
  | 'AUSENTE';

export type EstadoDestino = 'EN_ATENCION' | 'FINALIZADO' | 'CANCELADO' | 'AUSENTE';

export type ConjuntoEfectivo = 'PREFERENTE' | 'PREFERENTE_ENVEJECIDO' | 'REGULAR';

export interface LoginPeticion {
  username: string;
  password: string;
}

export interface AuthRespuesta {
  token: string;
  tipoToken: string;
  expiracion: string;
  usuarioId: number;
  username: string;
  nombreCompleto: string;
  rol: Rol;
}

export interface Usuario {
  id: number;
  username: string;
  nombreCompleto: string;
  rol: Rol;
  activo: boolean;
}

export interface UsuarioCrear {
  username: string;
  nombreCompleto: string;
  password: string;
  rol: Rol;
}

export interface UsuarioActualizar {
  nombreCompleto: string;
  password: string | null;
  rol: Rol;
  activo: boolean;
}

export interface Servicio {
  id: number;
  codigoPrefijo: string;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
}

export interface ServicioCrear {
  codigoPrefijo: string;
  nombre: string;
  descripcion: string | null;
}

export interface ServicioActualizar {
  nombre: string;
  descripcion: string | null;
  activo: boolean;
}

export interface Modulo {
  id: number;
  servicioId: number;
  servicioNombre: string;
  servicioPrefijo: string;
  nombre: string;
  activo: boolean;
  ocupado: boolean;
  turnoActivoId?: number | null;
  turnoActivoCodigo: string | null;
}

export interface ModuloCrear {
  servicioId: number;
  nombre: string;
}

export interface ModuloActualizar {
  servicioId: number;
  nombre: string;
  activo: boolean;
}

export interface Categoria {
  id: number;
  nombre: string;
  preferente: boolean;
  activo: boolean;
}

export interface EstadoCola {
  servicioId: number;
  servicioPrefijo: string;
  servicioNombre: string;
  fechaOperacion: string;
  ultimoConsecutivo: number;
  contadorPreferentes: number;
  umbralEnvejecimientoMin: number;
  proporcionPreferente: number;
}

export interface ParametrosCola {
  umbralEnvejecimientoMin: number;
}

export interface RegistroTurno {
  identificadorCliente: string;
  servicioId: number;
  categoriaId: number;
}

export interface CambioEstado {
  nuevoEstado: EstadoDestino;
  motivo: string | null;
}

export interface Turno {
  id: number;
  codigo: string;
  servicioId: number;
  servicioNombre: string;
  categoriaId: number;
  categoriaOriginalNombre: string;
  preferenteOriginal: boolean;
  preferenteEfectivo: boolean | null;
  envejecido: boolean | null;
  conjuntoEfectivo: ConjuntoEfectivo | null;
  moduloId: number | null;
  moduloNombre: string | null;
  operadorId: number | null;
  operadorUsername: string | null;
  fechaOperacion: string;
  consecutivo: number;
  identificadorCliente: string;
  estado: EstadoTurno;
  fechaIngreso: string;
  fechaLlamado: string | null;
  fechaInicioAtencion: string | null;
  fechaFinAtencion: string | null;
  fechaCancelacionAusencia: string | null;
  motivoCancelacionAusencia: string | null;
}

export interface Transicion {
  id: number;
  estadoAnterior: EstadoTurno | null;
  estadoNuevo: EstadoTurno;
  fechaCambio: string;
  operadorUsername: string | null;
  motivo: string | null;
}

export interface TurnoDetalle extends Turno {
  transiciones: Transicion[];
}

export interface SiguienteTurno {
  colaVacia: boolean;
  mensaje: string;
  turno: Turno | null;
  contadorPreferentesActual: number;
}

export interface TurnoCola {
  ordenEnCola: number;
  id: number;
  codigo: string;
  servicioId: number;
  servicioNombre: string;
  identificadorCliente: string;
  categoriaId: number;
  categoriaOriginalNombre: string;
  preferenteOriginal: boolean;
  preferenteEfectivo: boolean;
  envejecido: boolean;
  conjuntoEfectivo: ConjuntoEfectivo;
  fechaOperacion: string;
  fechaIngreso: string;
  tiempoEsperaMin: number;
  estado: EstadoTurno;
}

export interface TurnoResumen {
  id: number;
  codigo: string;
  fechaOperacion: string;
  servicioNombre: string;
  categoriaOriginalNombre: string;
  identificadorCliente: string;
  moduloNombre: string | null;
  operadorUsername: string | null;
  estado: EstadoTurno;
  fechaIngreso: string;
  fechaLlamado: string | null;
  fechaInicioAtencion: string | null;
  fechaFinAtencion: string | null;
  fechaCancelacionAusencia: string | null;
  motivoCancelacionAusencia: string | null;
}

export interface TurnoPublicoModulo {
  codigo: string;
  servicioNombre: string;
  moduloNombre: string;
  estado: EstadoTurno;
  fechaLlamado: string;
}

export interface TurnoPublicoCola {
  ordenEnCola: number;
  codigo: string;
  servicioNombre: string;
  estado: EstadoTurno;
}

export interface PantallaPublica {
  fechaOperacion: string;
  horaServidor: string;
  turnosEnModulos: TurnoPublicoModulo[];
  proximosEnEspera: TurnoPublicoCola[];
}

export interface Indicadores {
  fechaConsulta: string;
  servicioId: number | null;
  totalEmitidos: number;
  totalFinalizados: number;
  totalCancelados: number;
  totalAusentes: number;
  totalEnEspera: number;
  totalEnAtencionOLlamado: number;
  tiempoPromedioEsperaSegundos: number | null;
  tiempoPromedioEsperaFormateado: string | null;
}

export interface ErrorRespuesta {
  timestamp: string;
  status: number;
  error: string;
  mensaje: string;
  ruta: string;
  detallesValidacion: Record<string, string> | null;
}

export interface FiltroHistorial {
  fecha?: string | null;
  servicioId?: number | null;
  estado?: EstadoTurno | null;
}
