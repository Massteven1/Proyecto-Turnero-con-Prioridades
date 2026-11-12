package com.banco.app.entity;

// Estados posibles de un turno, con el flujo principal permitido:
// EN_ESPERA -> LLAMADO -> EN_ATENCION -> FINALIZADO
// ademas de las salidas CANCELADO (desde EN_ESPERA o LLAMADO) y AUSENTE (desde LLAMADO).
// FINALIZADO, CANCELADO y AUSENTE son terminales: definitivos y sin retorno a la cola.
public enum EstadoTurno {
    EN_ESPERA,
    LLAMADO,
    EN_ATENCION,
    FINALIZADO,
    CANCELADO,
    AUSENTE
}
