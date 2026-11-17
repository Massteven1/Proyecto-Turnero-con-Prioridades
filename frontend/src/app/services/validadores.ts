import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

// Longitud sobre el texto recortado, igual que @NotBlank + @Size del backend.
export function texto(min: number, max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const valor = String(control.value ?? '').trim();
    if (!valor) return { requerido: true };
    if (valor.length < min) return { corto: { min } };
    if (valor.length > max) return { largo: { max } };
    return null;
  };
}

// Contraseña: 8 a 72 caracteres y máximo 72 bytes en UTF-8 (límite de BCrypt).
export function contrasena(opcional = false): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const valor = String(control.value ?? '');
    if (!valor) return opcional ? null : { requerido: true };
    if (valor.length < 8) return { corto: { min: 8 } };
    if (valor.length > 72) return { largo: { max: 72 } };
    if (new TextEncoder().encode(valor).length > 72) return { bytes: true };
    return null;
  };
}

export function mensajeCampo(control: AbstractControl | null): string | null {
  if (!control || !control.errors || !(control.touched || control.dirty)) return null;
  const e = control.errors;
  if (e['requerido'] || e['required']) return 'Este campo es obligatorio.';
  if (e['corto']) return `Escribe al menos ${e['corto'].min} caracteres.`;
  if (e['largo']) return `Usa como máximo ${e['largo'].max} caracteres.`;
  if (e['maxlength']) return `Usa como máximo ${e['maxlength'].requiredLength} caracteres.`;
  if (e['bytes']) return 'La contraseña supera 72 bytes: usa menos tildes o símbolos especiales.';
  if (e['min']) return `El valor mínimo es ${e['min'].min}.`;
  if (e['max']) return `El valor máximo es ${e['max'].max}.`;
  if (e['pattern']) return 'El formato no es válido.';
  if (e['espacios']) return 'No puede contener espacios.';
  return 'Valor no válido.';
}

export function sinEspacios(control: AbstractControl): ValidationErrors | null {
  return /\s/.test(String(control.value ?? '')) ? { espacios: true } : null;
}
