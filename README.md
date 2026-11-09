# Clean TV — debloater para Android/Google TV desde el teléfono

App Android **para tu teléfono** que se conecta a tu propia Android TV / Google TV por **ADB sobre Wi‑Fi**
y te deja quitar el bloatware (telemetría, filas patrocinadas, apps preinstaladas) sin PC y sin root.

> Inspirada en el proyecto [CleanYourTV](https://github.com/Gathin23/cleanyourtv) (MIT), del que se reutilizan
> las listas de paquetes por modelo. Ver [`NOTICE`](NOTICE).

## Qué hace

- Se conecta a la TV por ADB TCP (`ip:5555`), usando una clave ADB propia que la app genera y guarda.
- Lista los paquetes instalados en la TV (`pm list packages`).
- Los compara con **458 perfiles** de debloat (457 modelos de TV/decodificador + una lista común
  multimarca segura) y te deja marcar/desmarcar, con el motivo de cada paquete.
- Muestra **descripción y nivel de riesgo** por paquete (Recommended/Advanced/Expert/Unsafe) usando
  la base de **UAD‑ng**, y marca los paquetes de **telemetría/rastreo**.
- Dos métodos, tú eliges:
  - **Desactivar** (`pm disable-user --user 0`): reversible, no borra nada. Para apps molestas.
  - **Desinstalar** (`pm uninstall --user 0`): quita la app para el usuario 0.
- Guarda un **respaldo** de lo que tocó y lo **restaura** (con `pm enable` o `install-existing`).
- **Bloqueo de anuncios** en toda la TV con un toque: DNS privado filtrante (AdGuard DNS,
  `dns.adguard-dns.com`), sin root ni app. Se activa/desactiva desde la app.
- Instala un launcher (**Projectivy**) y lo fija como pantalla de inicio.

## Launcher (Projectivy)

En la pantalla de paquetes, botón **"Instalar launcher (Projectivy) y fijar como inicio…"**:

1. Elige el APK de [Projectivy Launcher](https://play.google.com/store/apps/details?id=com.spocky.projengmenu)
   desde tu teléfono (el selector de archivos del sistema).
2. La app lo sube a la TV con `adb install` y lo fija con
   `cmd package set-home-activity com.spocky.projengmenu/...ui.home.MainActivity`.
3. Si la TV no acepta fijarlo por ADB, abre Projectivy una vez y elígelo como inicio desde Ajustes.

> Projectivy es de su autor (Spocky) y no se redistribuye en este repo: la app
> solo instala el APK que tú elijas con el selector de archivos.

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

## Aviso importante sobre decodificadores baratos

Algunas cajas Android baratas (AOSP, no certificadas por Google) vienen con **malware a nivel de
firmware/kernel** (caso BadBox / BadBox 2.0). Ese tipo de software **no se puede quitar** con
`pm disable`/`pm uninstall` ni por ADB: la única solución real es reflashear firmware limpio o
reemplazar el equipo. Esta app puede marcar paquetes sospechosos, pero no elimina malware de firmware.

## Licencia

**GPL‑3.0** — ver [`LICENSE`](LICENSE). Este proyecto integra la base de datos de paquetes de
[UAD‑ng](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)
(GPL‑3.0), por lo que el conjunto se distribuye bajo GPL‑3.0. Las listas por modelo derivan de
[CleanYourTV](https://github.com/Gathin23/cleanyourtv) (MIT). Ver [`NOTICE`](NOTICE).
