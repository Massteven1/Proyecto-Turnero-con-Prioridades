package com.banco.app.config;

import java.time.ZoneId;

// Zona horaria oficial del sistema, exigida por la documentacion:
// America/Bogota (UTC-5) para todas las marcas temporales del backend.
// DATETIME de MariaDB no guarda zona, asi que Java la resuelve aqui.
public final class ZonaHoraria {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private ZonaHoraria() {
        // Constantes de clase: no se instancia
    }
}
