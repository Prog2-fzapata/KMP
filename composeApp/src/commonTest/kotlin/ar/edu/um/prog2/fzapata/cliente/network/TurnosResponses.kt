package ar.edu.um.prog2.fzapata.cliente.network

// Respuestas reales de turnos (2026-11-25, profesional 207) que usan varias pruebas; traen también campos que la app ignora
object TurnosResponses {
    const val CREATED = """{"id":2,"reservationProcessId":"process-10e3d0f6-5e76-4f80-910a-f6b0d46d2f90","status":"WAITING_PHONE","professionalId":207,"professionalFirstName":"Diego","professionalLastName":"Alvarez","appointmentDate":"2027-01-25","startTime":"18:00:00","endTime":"18:30:00","expiresAt":"2026-11-25T01:09:47.626145576Z","patientPhone":null,"failureReason":null,"cancellationReason":null,"canSubmitPhone":false,"createdAt":"2026-11-25T00:59:47.357907Z","updatedAt":"2026-11-25T00:59:48.038325906Z"}"""
    const val TAKEN = """{"detail":"El horario ya no está disponible","instance":"/api/reservations","status":409,"title":"Conflict","path":"/api/reservations","code":"SLOT_UNAVAILABLE"}"""
    const val NAME_REQUIRED = """{"detail":"La cuenta necesita nombre y apellido de al menos 2 caracteres para reservar","instance":"/api/reservations","status":400,"title":"Bad Request","path":"/api/reservations","code":"PATIENT_NAME_REQUIRED"}"""
}
