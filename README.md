# Instagram Reels Blocker

Aplicación Android que bloquea la sección de Reels en Instagram con control por horario y contraseña.

## Características
- Bloquea acceso a Reels en Instagram
- Temporizador de 10 minutos en horario específico
- Protección por contraseña
- Servicio en segundo plano

## Requisitos
- Android 7.0+ (API 24+)
- Permisos de Accesibilidad
- Permiso de superposición de pantalla

## Instalación
1. Habilitar "Instalar apps desconocidas" en Configuración
2. Instalar el APK
3. Conceder permisos de Accesibilidad
4. Conceder permiso de superposición
5. Configurar contraseña y horario

## Estructura del proyecto
```
app/
├── MainActivity.kt - Configuración inicial
├── ReelsBlockerService.kt - Servicio de accesibilidad
├── BlockerOverlay.kt - Pantalla de bloqueo
├── SettingsManager.kt - Gestión de configuración
└── PasswordManager.kt - Manejo de contraseña
```

## Nota importante
Esta app requiere permisos sensibles. Úsala responsablemente y solo en tu propio dispositivo.