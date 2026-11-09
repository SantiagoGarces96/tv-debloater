# Clean TV — debloater para Android/Google TV desde el teléfono

App Android **para tu teléfono** que se conecta a tu propia Android TV / Google TV por **ADB sobre Wi‑Fi**
y te deja quitar el bloatware (telemetría, filas patrocinadas, apps preinstaladas) sin PC y sin root.

> Inspirada en el proyecto [CleanYourTV](https://github.com/Gathin23/cleanyourtv) (MIT), del que se reutilizan
> las listas de paquetes por modelo. Ver [`NOTICE`](NOTICE).

## Qué hace

- Se conecta a la TV por ADB TCP (`ip:5555`), usando una clave ADB propia que la app genera y guarda.
- Lista los paquetes instalados en la TV (`pm list packages`).
- Los compara con listas de debloat conocidas (en `app/src/main/assets/lists/`) y te deja marcar/desmarcar.
- Aplica **`pm uninstall --user 0 <paquete>`** a lo que elijas (se puede revertir con
  `pm install-existing` o un reset de fábrica).
- Guarda un **respaldo** de lo que tocó, para restaurar después.
- (Fase 2) Instala un launcher limpio y lo fija como pantalla de inicio.

## Importante / alcance

- **Solo para dispositivos tuyos.** Esto usa la depuración ADB que tú habilitas; no rompe ni evita ninguna
  protección del fabricante.
- `pm uninstall --user 0` **no borra el APK del sistema**: lo quita para el usuario actual. Vuelve con
  `pm install-existing <paquete>` o un reset de fábrica.
- Quitar paquetes de actualización desactiva las actualizaciones automáticas. Revisa cada lista antes de aplicar.

## Cómo preparar la TV

1. Ajustes → Preferencias del dispositivo → Acerca de → pulsa **Build/Compilación** 7 veces.
2. Opciones de desarrollador → activa **Depuración USB** y, si aparece, **Depuración por red / ADB por red**.
3. Deja la TV encendida y anota su IP (Ajustes → Red).

## Requisitos de desarrollo

- Android Studio (Ladybug o más reciente) con JDK 17.
- Android SDK Platform 35 y Build‑Tools 35.
- Teléfono con Android 8+ (minSdk 26) en la **misma red Wi‑Fi** que la TV.

## Build

```bash
git clone https://github.com/SantiagoGarces96/tv-debloater
cd tv-debloater
# Abrir en Android Studio (genera el gradle wrapper al sincronizar) o:
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Estado

Proyecto en construcción, se sube por partes. Ver commits.

## Licencia

MIT — ver [`LICENSE`](LICENSE). Las listas de paquetes derivan de CleanYourTV (MIT), ver [`NOTICE`](NOTICE).
