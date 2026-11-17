import { HttpErrorResponse } from '@angular/common/http';
import { ErrorRespuesta } from './modelos';

export interface ErrorApi {
  status: number;
  mensaje: string;
  campos: Record<string, string>;
}

const POR_DEFECTO: Record<number, string> = {
  400: 'Revisa los datos: hay campos con valores no válidos.',
  401: 'Tu sesión no es válida. Inicia sesión de nuevo.',
  403: 'Tu rol no permite esta acción.',
  404: 'El registro ya no existe.',
  409: 'La operación entra en conflicto con el estado actual. Actualiza e intenta de nuevo.',
  429: 'Demasiados intentos. Espera unos minutos e intenta de nuevo.',
};

// Traduce cualquier error HTTP al mensaje del ErrorResponseDTO del backend,
// que ya viene redactado para el usuario.
export function leerError(error: unknown): ErrorApi {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return {
        status: 0,
        mensaje:
          'No hay conexión con el servidor. Comprueba que el backend esté en marcha en localhost:8080.',
        campos: {},
      };
    }
    const cuerpo = error.error as Partial<ErrorRespuesta> | string | null;
    const mensaje =
      cuerpo && typeof cuerpo === 'object' && cuerpo.mensaje
        ? cuerpo.mensaje
        : (POR_DEFECTO[error.status] ?? 'El servidor no pudo completar la operación.');
    const campos =
      cuerpo && typeof cuerpo === 'object' && cuerpo.detallesValidacion
        ? cuerpo.detallesValidacion
        : {};
    return { status: error.status, mensaje, campos };
  }
  return { status: -1, mensaje: 'Ocurrió un error inesperado en la aplicación.', campos: {} };
}
