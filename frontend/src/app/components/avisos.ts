import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Avisos } from '../services/avisos';

@Component({
  selector: 'app-avisos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="avisos" aria-live="polite" aria-atomic="false">
      @for (aviso of avisos.lista(); track aviso.id) {
        <div class="aviso" [class]="'aviso aviso--' + aviso.tipo" [attr.role]="aviso.tipo === 'error' ? 'alert' : 'status'">
          <p>{{ aviso.texto }}</p>
          <button type="button" class="aviso__cerrar" (click)="avisos.cerrar(aviso.id)" aria-label="Cerrar aviso">×</button>
        </div>
      }
    </div>
  `,
  styles: `
    .avisos {
      position: fixed;
      right: 1rem;
      bottom: 1rem;
      z-index: 50;
      display: grid;
      gap: 0.5rem;
      width: min(26rem, calc(100vw - 2rem));
    }
    .aviso {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      padding: 0.75rem 0.75rem 0.75rem 1rem;
      border-left: 5px solid var(--pizarra-tenue);
      border-radius: 6px;
      background: var(--pizarra);
      color: var(--pizarra-texto);
      font-size: var(--t-s);
      animation: aviso-entra 160ms ease-out;
    }
    .aviso p { flex: 1; }
    .aviso--exito { border-left-color: #5fc49a; }
    .aviso--error { border-left-color: #f08a76; }
    .aviso--info { border-left-color: var(--led); }
    .aviso__cerrar {
      border: 0;
      background: transparent;
      color: var(--pizarra-tenue);
      font-size: 1.25rem;
      line-height: 1;
      cursor: pointer;
    }
    @keyframes aviso-entra { from { transform: translateY(0.5rem); opacity: 0; } }
  `,
})
export class AvisosHost {
  protected readonly avisos = inject(Avisos);
}
