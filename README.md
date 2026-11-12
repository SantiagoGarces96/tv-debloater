# SG Alfa — limpia tu TV y decodificador desde el teléfono

<p align="center"><img src="docs/logo.svg" width="440" alt="SG Alfa"></p>

[![CI](https://github.com/SantiagoGarces96/tv-debloater/actions/workflows/ci.yml/badge.svg)](https://github.com/SantiagoGarces96/tv-debloater/actions/workflows/ci.yml)

**SG Alfa** es una app Android **para tu teléfono** que limpia tu propia tele o decodificador: quita el
bloatware (telemetría, filas patrocinadas, apps preinstaladas), **bloquea anuncios** y pone una
**pantalla de inicio propia y personalizable**. Sin PC y, en Android, **sin root**.

> Proyecto propio e independiente. Reutiliza datos abiertos con crédito (ver [`NOTICE`](NOTICE)) e
> iremos migrando a datos propios.

## Vistas

<p align="center"><img src="docs/preview.svg" width="900" alt="Vista previa de la app"></p>

<sub>Vista previa de la interfaz (mockup). La app aún no se ha probado en una TV real.</sub>

## Plataformas

Al abrir, la app pregunta qué tienes:

- **Android TV / Google TV / Decodificador** → limpieza completa por **ADB** (lo que describe este README).
  Al conectar, detecta además si es **Televisor**, **Decodificador / TV box** o **Fire TV**.
- **LG (webOS)** → no es Android ni usa ADB; se limpia poniendo un launcher propio vía **Modo
  Desarrollador**. La app conecta por SSH e instala el launcher.
- **Samsung (Tizen)** → usa **SDB** (no ADB) y Modo Desarrollador. La limpieza real necesita root y es
  específica de cada modelo, así que la app **diagnostica la conexión y te guía** paso a paso.

**A futuro:** soporte para **teléfonos Android** (debloat del propio móvil).

## Qué hace (Android)

- **Encuentra la TV sola en el Wi‑Fi** (escaneo del puerto ADB), o le pones la IP a mano. Recuerda la última.
- Se conecta por ADB TCP (`ip:5555`) con una clave ADB propia que la app genera y guarda.
- Compara los paquetes instalados con **458 perfiles** (457 modelos de TV/decodificador + una lista común
  multimarca segura), **agrupados** en Anuncios / Te espían / Extras de marca / Apps de sobra.
- Muestra **descripción y nivel de riesgo** por paquete (seguro / con cuidado / avanzado / riesgo) y marca
  lo que **te espía** (telemetría/rastreo). Arriba, un **resumen de hallazgos**.
- **Limpiar en un toque** (lo claramente molesto), o elegir en detalle. Dos métodos:
  - **Apagar** (`pm disable-user --user 0`): reversible, no borra nada.
  - **Borrar** (`pm uninstall --user 0`): lo quita para el usuario 0.
- Pide **confirmación** antes de aplicar y guarda un **respaldo** para **deshacer** (con `pm enable` o
  `install-existing`).
- **Bloqueo de anuncios** en toda la TV (DNS filtrante AdGuard por ADB).

## Pantalla de inicio: SG Alfa Home

La app instala **[SG Alfa Home](https://github.com/SantiagoGarces96/sg-alfa-tv)** (pantalla de inicio propia,
con Alfi, temas y actualización automática), descargando siempre su última versión. Quita la pantalla de inicio antigua (`tv.clean.home`) si estaba.

- Diseño retro propio con 6 temas (y el tuyo), Alfi la mascota 8-bit, dock de favoritas y todas tus apps.
- **Protección SG Alfa** (en sus Ajustes): bloqueo de anuncios y pop-ups por DNS (VPN local, sin root) y
  **automatizaciones** estilo MacroDroid que se crean desde esta app.

## Importante / alcance

- **Solo para dispositivos tuyos.** Usa la depuración/Modo Desarrollador que tú habilitas; no rompe ni
  evita ninguna protección del fabricante.
- `pm uninstall --user 0` **no borra el APK del sistema**: lo quita para el usuario actual. Vuelve con
  `pm install-existing <paquete>` o un reset de fábrica.
- Quitar paquetes de actualización desactiva las actualizaciones automáticas. Revisa cada lista antes de aplicar.

## Decodificadores baratos (aviso)

Algunas cajas Android baratas (AOSP, no certificadas por Google) vienen con **malware a nivel de
firmware/kernel** (caso BadBox / BadBox 2.0). Ese software **no se puede quitar** con `pm disable`/`pm
uninstall` ni por ADB: la única solución real es reflashear firmware limpio o reemplazar el equipo. La app
puede marcar paquetes sospechosos, pero no elimina malware de firmware.

## Cómo preparar la TV (Android)

1. Ajustes → Preferencias del dispositivo → Acerca de → pulsa **Build/Compilación** 7 veces.
2. Opciones de desarrollador → activa **Depuración por red / ADB por red**.
3. Deja la TV encendida, en la misma Wi‑Fi que el teléfono.

## Build

```bash
git clone https://github.com/SantiagoGarces96/tv-debloater
cd tv-debloater
./gradlew assembleDebug   # requiere JDK 17 y Android SDK 35
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. Cada tag `vX.Y.Z` publica una release con el
APK automáticamente (GitHub Actions).

## Estado

En construcción, se sube por partes. **Aún sin probar en una TV real** — el siguiente paso clave es
verificarlo en hardware (sobre todo el bloqueo DNS).

## Licencia

**GPL‑3.0** — ver [`LICENSE`](LICENSE). Integra la base de paquetes de
[UAD‑ng](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)
(GPL‑3.0), por lo que el conjunto es GPL‑3.0. Las listas por modelo derivan de
[CleanYourTV](https://github.com/Gathin23/cleanyourtv) (MIT). Ver [`NOTICE`](NOTICE).
