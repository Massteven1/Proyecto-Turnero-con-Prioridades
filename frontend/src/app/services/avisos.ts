import { Service, signal } from '@angular/core';

export type TipoAviso = 'exito' | 'error' | 'info';

export interface Aviso {
  id: number;
  tipo: TipoAviso;
  texto: string;
}

// Mensajes breves que confirman una acción o explican un rechazo del backend.
@Service()
export class Avisos {
  private siguienteId = 1;
  readonly lista = signal<Aviso[]>([]);

  exito(texto: string): void {
    this.mostrar('exito', texto, 5000);
  }

  info(texto: string): void {
    this.mostrar('info', texto, 6000);
  }

  error(texto: string): void {
    this.mostrar('error', texto, 9000);
  }

  cerrar(id: number): void {
    this.lista.update((avisos) => avisos.filter((a) => a.id !== id));
  }

  private mostrar(tipo: TipoAviso, texto: string, duracion: number): void {
    const id = this.siguienteId++;
    this.lista.update((avisos) => [...avisos.slice(-3), { id, tipo, texto }]);
    setTimeout(() => this.cerrar(id), duracion);
  }
}
