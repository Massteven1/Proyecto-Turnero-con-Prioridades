import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ETIQUETA_ROL } from '../services/etiquetas';
import { hora } from '../services/fechas';
import { SesionService } from '../services/sesion';
import { MarcaBanco } from './marca';

// Marco de las vistas autenticadas: barra con la navegación permitida por rol,
// la cuenta en uso, el vencimiento de la sesión y el cierre de sesión.
@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MarcaBanco],
  selector: 'app-main',
  styleUrl: './main.css',
  templateUrl: './main.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Main {
  private readonly sesion = inject(SesionService);

  protected readonly usuario = this.sesion.sesion;
  protected readonly esAdmin = this.sesion.esAdmin;
  protected readonly rol = computed(() => {
    const rol = this.sesion.rol();
    return rol ? ETIQUETA_ROL[rol] : '';
  });
  protected readonly vence = computed(() => hora(this.usuario()?.expiracion));

  protected cerrarSesion(): void {
    this.sesion.cerrar();
  }
}
