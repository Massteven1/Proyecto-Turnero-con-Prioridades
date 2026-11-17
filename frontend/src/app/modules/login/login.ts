import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CodigoTurno } from '../../components/codigo-turno';
import { MarcaBanco } from '../../components/marca';
import { TurneroApi } from '../../services/api';
import { leerError } from '../../services/errores';
import { PantallaPublica } from '../../services/modelos';
import { SesionService } from '../../services/sesion';
import { contrasena, mensajeCampo, texto } from '../../services/validadores';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, CodigoTurno, MarcaBanco],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login implements OnInit {
  private readonly api = inject(TurneroApi);
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);

  // Query params: ?sesion=expirada|invalida y ?volver=/ruta
  readonly sesionParam = input<string | undefined>(undefined, { alias: 'sesion' });
  readonly volver = input<string | undefined>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    username: ['', [texto(3, 50)]],
    password: ['', [contrasena()]],
    recordar: [false],
  });

  protected readonly verContrasena = signal(false);
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly sala = signal<PantallaPublica | null>(null);

  protected readonly avisoSesion = computed(() => {
    switch (this.sesionParam()) {
      case 'expirada':
        return 'Tu sesión expiró. Inicia sesión de nuevo para continuar.';
      case 'invalida':
        return 'Tu sesión se cerró porque la cuenta cambió de rol o fue desactivada. Inicia sesión de nuevo.';
      default:
        return null;
    }
  });

  protected readonly llamado = computed(
    () => this.sala()?.turnosEnModulos.find((t) => t.estado === 'LLAMADO') ?? null,
  );
  protected readonly enEspera = computed(() => this.sala()?.proximosEnEspera.length ?? 0);
  protected readonly enVentanilla = computed(() => this.sala()?.turnosEnModulos.length ?? 0);

  ngOnInit(): void {
    const recordado = this.sesion.usuarioRecordado();
    if (recordado) {
      this.form.patchValue({ username: recordado, recordar: true });
    }
    // Vista previa de la sala de espera: endpoint público, nunca lleva token
    this.api.pantalla().subscribe({ next: (datos) => this.sala.set(datos), error: () => {} });
  }

  protected errorCampo(nombre: 'username' | 'password'): string | null {
    return mensajeCampo(this.form.controls[nombre]);
  }

  protected async ingresar(): Promise<void> {
    this.form.markAllAsTouched();
    this.error.set(null);
    if (this.form.invalid || this.enviando()) return;

    const { username, password, recordar } = this.form.getRawValue();
    this.enviando.set(true);
    try {
      const respuesta = await firstValueFrom(this.api.login({ username: username.trim(), password }));
      this.sesion.recordarUsuario(recordar ? respuesta.username : null);
      this.sesion.iniciar(respuesta);
      await this.router.navigateByUrl(this.destino(respuesta.rol));
    } catch (e) {
      const err = leerError(e);
      this.error.set(err.status === 401 ? 'Usuario o contraseña incorrectos.' : err.mensaje);
      this.form.controls.password.reset('');
    } finally {
      this.enviando.set(false);
    }
  }

  // Solo rutas internas y permitidas para el rol (evita redirecciones abiertas)
  private destino(rol: 'ADMIN' | 'OPERADOR'): string {
    const volver = this.volver();
    const permitida =
      volver &&
      volver.startsWith('/') &&
      !volver.startsWith('//') &&
      !volver.startsWith('/login') &&
      (rol === 'ADMIN' || volver.startsWith('/operacion'));
    return permitida ? volver : this.sesion.rutaInicial(rol);
  }

}
