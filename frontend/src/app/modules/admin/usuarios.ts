import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Confirmacion } from '../../components/confirmacion';
import { ActivoBadge } from '../../components/estado-turno';
import { TurneroApi } from '../../services/api';
import { Avisos } from '../../services/avisos';
import { leerError } from '../../services/errores';
import { ETIQUETA_ROL } from '../../services/etiquetas';
import { Rol, Usuario } from '../../services/modelos';
import { SesionService } from '../../services/sesion';
import { contrasena, mensajeCampo, sinEspacios, texto } from '../../services/validadores';

type Filtro = 'todos' | 'activos' | 'inactivos';

// Cuentas del personal (HU-02). El username es inmutable, la contraseña se
// guarda con BCrypt en el backend y nunca vuelve en las respuestas.
@Component({
  selector: 'app-usuarios',
  imports: [ReactiveFormsModule, ActivoBadge, Confirmacion],
  templateUrl: './usuarios.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Usuarios implements OnInit {
  private readonly api = inject(TurneroApi);
  private readonly avisos = inject(Avisos);
  private readonly sesion = inject(SesionService);
  private readonly confirmacion = viewChild.required(Confirmacion);

  protected readonly usuarios = signal<Usuario[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly filtro = signal<Filtro>('todos');
  protected readonly editando = signal<Usuario | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorForm = signal<string | null>(null);
  protected readonly roles: Rol[] = ['OPERADOR', 'ADMIN'];
  protected readonly etiquetaRol = ETIQUETA_ROL;
  protected readonly yo = computed(() => this.sesion.sesion()?.usuarioId ?? null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    username: ['', [texto(3, 50), sinEspacios]],
    nombreCompleto: ['', [texto(3, 100)]],
    password: ['', [contrasena()]],
    rol: ['OPERADOR' as Rol, [Validators.required]],
    activo: [true],
  });

  protected readonly visibles = computed(() => {
    const filtro = this.filtro();
    return this.usuarios().filter((u) =>
      filtro === 'todos' ? true : filtro === 'activos' ? u.activo : !u.activo,
    );
  });

  ngOnInit(): void {
    void this.cargar();
  }

  protected nuevo(): void {
    this.editando.set(null);
    this.errorForm.set(null);
    this.form.reset({ username: '', nombreCompleto: '', password: '', rol: 'OPERADOR', activo: true });
    this.form.controls.username.enable();
    this.form.controls.password.setValidators([contrasena()]);
    this.form.controls.password.updateValueAndValidity();
  }

  protected editar(usuario: Usuario): void {
    this.editando.set(usuario);
    this.errorForm.set(null);
    this.form.reset({
      username: usuario.username,
      nombreCompleto: usuario.nombreCompleto,
      password: '',
      rol: usuario.rol,
      activo: usuario.activo,
    });
    this.form.controls.username.disable();
    // En la edición la contraseña es opcional: vacía conserva la actual
    this.form.controls.password.setValidators([contrasena(true)]);
    this.form.controls.password.updateValueAndValidity();
  }

  protected errorCampo(nombre: 'username' | 'nombreCompleto' | 'password'): string | null {
    return mensajeCampo(this.form.controls[nombre]);
  }

  protected async guardar(): Promise<void> {
    this.form.markAllAsTouched();
    this.errorForm.set(null);
    if (this.form.invalid || this.guardando()) return;
    const v = this.form.getRawValue();
    const actual = this.editando();
    if (actual && actual.activo && !v.activo && !(await this.confirmarBaja(actual))) return;

    this.guardando.set(true);
    try {
      if (actual) {
        await firstValueFrom(
          this.api.actualizarUsuario(actual.id, {
            nombreCompleto: v.nombreCompleto.trim(),
            password: v.password ? v.password : null,
            rol: v.rol,
            activo: v.activo,
          }),
        );
        this.avisos.exito(`Usuario ${actual.username} actualizado.`);
      } else {
        await firstValueFrom(
          this.api.crearUsuario({
            username: v.username.trim(),
            nombreCompleto: v.nombreCompleto.trim(),
            password: v.password,
            rol: v.rol,
          }),
        );
        this.avisos.exito(`Usuario ${v.username.trim()} creado como ${ETIQUETA_ROL[v.rol].toLowerCase()}.`);
      }
      this.nuevo();
      await this.cargar();
    } catch (e) {
      const err = leerError(e);
      const campos = Object.values(err.campos);
      this.errorForm.set(campos.length ? `${err.mensaje} ${campos.join(' ')}` : err.mensaje);
    } finally {
      this.guardando.set(false);
    }
  }

  protected async cambiarActivo(usuario: Usuario): Promise<void> {
    const activar = !usuario.activo;
    if (!activar && !(await this.confirmarBaja(usuario))) return;
    try {
      await firstValueFrom(
        this.api.actualizarUsuario(usuario.id, {
          nombreCompleto: usuario.nombreCompleto,
          password: null,
          rol: usuario.rol,
          activo: activar,
        }),
      );
      this.avisos.exito(`Cuenta ${usuario.username} ${activar ? 'activada' : 'desactivada'}.`);
      if (this.editando()?.id === usuario.id) this.nuevo();
      await this.cargar();
    } catch (e) {
      this.avisos.error(leerError(e).mensaje);
    }
  }

  private confirmarBaja(usuario: Usuario): Promise<boolean> {
    const propia = usuario.id === this.yo();
    return this.confirmacion().abrir({
      titulo: `Desactivar la cuenta ${usuario.username}`,
      mensaje:
        (propia ? 'Es tu propia cuenta: tu sesión se cerrará en la siguiente acción. ' : '') +
        'La persona no podrá iniciar sesión y su sesión abierta dejará de funcionar. No se puede desactivar una cuenta con un turno en curso ni al único administrador activo.',
      accion: 'Desactivar cuenta',
      peligro: true,
    });
  }

  private async cargar(): Promise<void> {
    try {
      this.usuarios.set(await firstValueFrom(this.api.usuarios()));
      this.error.set(null);
    } catch (e) {
      this.error.set(leerError(e).mensaje);
    } finally {
      this.cargando.set(false);
    }
  }
}
