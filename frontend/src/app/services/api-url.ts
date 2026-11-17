import { InjectionToken } from '@angular/core';

// Raíz de la API REST del backend Spring Boot. El backend solo acepta el
// origen http://localhost:4200 por CORS (SecurityConfig).
export const API_URL = new InjectionToken<string>('API_URL', {
  factory: () => 'http://localhost:8080/api/v1',
});
