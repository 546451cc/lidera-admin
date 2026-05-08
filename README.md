# Lidera Tickets

App Android **nativa por API** para el check-in de Hi.Events, personalizada para Lidera Fest ICA.

## Qué hace

- Login contra la API de Hi.Events: `POST /api/auth/login`
- Carga eventos por API: `GET /api/events`
- Carga listas de check-in por API: `GET /api/events/{event_id}/check-in-lists`
- Escanea QR con cámara nativa inline usando ZXing, sin abrir una Activity externa
- Hace check-in por API: `POST /api/public/check-in-lists/{short_id}/check-ins`
- Permite código manual tipo `A-XXXX`
- Guarda sesión/token localmente para no iniciar sesión cada vez
- Usa logo e icono de Lidera Fest

## Cómo generar el APK

Sube este proyecto a GitHub. En la pestaña **Actions**, ejecuta o espera el workflow **Build Lidera Tickets APK**. Descarga el artifact **LideraTickets-debug-apk** y dentro estará el APK.

## Uso

1. Abre la app.
2. Escribe la URL base de Hi.Events, por ejemplo `https://tickets.tudominio.com`.
3. Inicia sesión con correo y contraseña.
4. Selecciona el evento.
5. Selecciona la lista de check-in/invitados.
6. Escanea tickets.

Esta versión ya no usa WebView para login ni para seleccionar eventos/listas.

## Notas de la v2.0.2

- Cambia el scanner QR a modo inline dentro de la misma pantalla para evitar cierres al aceptar permisos en Xiaomi/MIUI.
- Incluye una keystore debug fija en `app/lidera-debug.keystore`, para que los próximos APK de GitHub Actions puedan actualizarse encima sin conflicto de firma.
- Fuerza Node 24 en GitHub Actions para evitar el warning de Node.js 20 deprecated.

## v2.0.3

- Corrige el error de compilación causado por un fragmento residual de `onActivityResult`.
