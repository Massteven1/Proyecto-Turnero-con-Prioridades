import { ConjuntoEfectivo, EstadoTurno, Rol } from './modelos';

export const ETIQUETA_ESTADO: Record<EstadoTurno, string> = {
  EN_ESPERA: 'En espera',
  LLAMADO: 'Llamado',
  EN_ATENCION: 'En atención',
  FINALIZADO: 'Finalizado',
  CANCELADO: 'Cancelado',
  AUSENTE: 'Ausente',
};

export const CLASE_ESTADO: Record<EstadoTurno, string> = {
  EN_ESPERA: 'espera',
  LLAMADO: 'llamado',
  EN_ATENCION: 'atencion',
  FINALIZADO: 'finalizado',
  CANCELADO: 'cancelado',
  AUSENTE: 'ausente',
};

export const ESTADOS: EstadoTurno[] = [
  'EN_ESPERA',
  'LLAMADO',
  'EN_ATENCION',
  'FINALIZADO',
  'CANCELADO',
  'AUSENTE',
];

export const ETIQUETA_CONJUNTO: Record<ConjuntoEfectivo, string> = {
  PREFERENTE: 'Preferente',
  PREFERENTE_ENVEJECIDO: 'Preferente por espera',
  REGULAR: 'Regular',
};

export const CLASE_CONJUNTO: Record<ConjuntoEfectivo, string> = {
  PREFERENTE: 'preferente',
  PREFERENTE_ENVEJECIDO: 'envejecido',
  REGULAR: 'regular',
};

export const ETIQUETA_ROL: Record<Rol, string> = {
  ADMIN: 'Administrador',
  OPERADOR: 'Operador',
};

export function esActivo(estado: EstadoTurno): boolean {
  return estado === 'LLAMADO' || estado === 'EN_ATENCION';
}

export function esTerminal(estado: EstadoTurno): boolean {
  return estado === 'FINALIZADO' || estado === 'CANCELADO' || estado === 'AUSENTE';
}
