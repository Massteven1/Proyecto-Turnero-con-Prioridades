// El backend envía LocalDateTime/LocalDate en hora de America/Bogota y sin
// zona. Se formatean leyendo el texto tal cual, sin convertir a la zona del
// navegador, para que la hora mostrada sea siempre la de la sucursal.

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const MESES_LARGOS = [
  'enero',
  'febrero',
  'marzo',
  'abril',
  'mayo',
  'junio',
  'julio',
  'agosto',
  'septiembre',
  'octubre',
  'noviembre',
  'diciembre',
];
const DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];

// America/Bogota no tiene horario de verano: siempre UTC-5.
const DESFASE_BOGOTA = '-05:00';

export function hora(valor: string | null | undefined): string {
  if (!valor) return '—';
  const tiempo = valor.split('T')[1];
  return tiempo ? tiempo.slice(0, 5) : '—';
}

export function horaConSegundos(valor: string | null | undefined): string {
  if (!valor) return '—';
  const tiempo = valor.split('T')[1];
  return tiempo ? tiempo.slice(0, 8) : '—';
}

export function fecha(valor: string | null | undefined): string {
  if (!valor) return '—';
  const [anio, mes, dia] = valor.slice(0, 10).split('-');
  return `${Number(dia)} ${MESES[Number(mes) - 1]} ${anio}`;
}

export function fechaHora(valor: string | null | undefined): string {
  if (!valor) return '—';
  return `${fecha(valor)}, ${hora(valor)}`;
}

export function fechaLarga(valor: string | null | undefined): string {
  if (!valor) return '';
  const [anio, mes, dia] = valor.slice(0, 10).split('-').map(Number);
  const diaSemana = new Date(Date.UTC(anio, mes - 1, dia)).getUTCDay();
  return `${DIAS[diaSemana]} ${dia} de ${MESES_LARGOS[mes - 1]}`;
}

// Instante absoluto (ms) de un LocalDateTime de Bogotá.
export function instanteBogota(valor: string): number {
  return Date.parse(valor.slice(0, 23) + DESFASE_BOGOTA);
}

// Hora actual de Bogotá (HH:mm), sin depender de la zona del navegador.
export function horaActualBogota(): string {
  return new Intl.DateTimeFormat('es-CO', {
    timeZone: 'America/Bogota',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(new Date());
}

// Fecha de hoy en Bogotá con formato YYYY-MM-DD.
export function hoyBogota(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Bogota' }).format(new Date());
}

// Minutos completos entre dos LocalDateTime.
export function minutosEntre(desde: string | null, hasta: string | null): number | null {
  if (!desde || !hasta) return null;
  return Math.max(0, Math.floor((instanteBogota(hasta) - instanteBogota(desde)) / 60000));
}
