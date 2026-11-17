import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from './api-url';
import { authInterceptor } from './auth-interceptor';
import { SesionService } from './sesion';

describe('authInterceptor', () => {
  const api = 'http://api.test/api/v1';
  let http: HttpClient;
  let backend: HttpTestingController;
  const sesion = { token: () => 'jwt-de-prueba', terminar: vi.fn() };

  beforeEach(() => {
    sesion.terminar.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: api },
        { provide: SesionService, useValue: sesion },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('agrega el Bearer a las rutas protegidas', () => {
    http.get(`${api}/turnos`).subscribe();
    const peticion = backend.expectOne(`${api}/turnos`);
    expect(peticion.request.headers.get('Authorization')).toBe('Bearer jwt-de-prueba');
    peticion.flush([]);
  });

  it('no envía el token a la pantalla pública', () => {
    http.get(`${api}/publico/pantalla`).subscribe();
    const peticion = backend.expectOne(`${api}/publico/pantalla`);
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({});
  });

  it('no envía el token al login', () => {
    http.post(`${api}/auth/login`, {}).subscribe();
    const peticion = backend.expectOne(`${api}/auth/login`);
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({});
  });

  it('un 401 de la pantalla pública no cierra la sesión', () => {
    http.get(`${api}/publico/pantalla`).subscribe({ error: () => {} });
    backend.expectOne(`${api}/publico/pantalla`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(sesion.terminar).not.toHaveBeenCalled();
  });

  it('un 401 de una ruta protegida cierra la sesión', () => {
    http.get(`${api}/usuarios`).subscribe({ error: () => {} });
    backend.expectOne(`${api}/usuarios`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(sesion.terminar).toHaveBeenCalledWith('invalida');
  });

  it('no toca peticiones a otros orígenes', () => {
    http.get('https://otro.test/recurso').subscribe();
    const peticion = backend.expectOne('https://otro.test/recurso');
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({});
  });
});
