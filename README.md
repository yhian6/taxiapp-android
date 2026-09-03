# TaxiApp Android

Aplicacion movil Android para fidelizacion de pasajeros en una ruta de transporte local entre Cura Mori y Catacaos, Piura.

## Funcionalidades

- Registro e inicio de sesion de pasajeros con Firebase Authentication.
- Escaneo QR de unidades vehiculares.
- Registro de viajes con acumulacion automatica de puntos.
- Pantalla de exito con animacion Lottie despues de confirmar un viaje.
- Historial de viajes.
- Pantalla de puntos y recompensas.
- Perfil de pasajero con edicion de datos, modo oscuro y cierre de sesion.
- Reportes vinculados a unidad, placa y conductor.
- Integracion con Firebase Realtime Database.

## Tecnologias

- Java
- Android Studio
- Firebase Authentication
- Firebase Realtime Database
- ZXing Android Embedded
- Lottie Android
- Material Components

## Configuracion Firebase

Este repositorio no incluye el archivo real `app/google-services.json` por seguridad.

Para ejecutar el proyecto:

1. Crea un proyecto en Firebase.
2. Registra una app Android con package name `com.yhian.taxiapp`.
3. Descarga `google-services.json`.
4. Coloca el archivo en `app/google-services.json`.
5. Activa Firebase Authentication y Realtime Database.

Puedes usar `app/google-services.json.example` como referencia de estructura.

## Estado del proyecto

Proyecto en desarrollo, orientado a una propuesta comercial para un comite de transporte de pasajeros.
