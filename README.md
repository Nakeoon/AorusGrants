# ⚡ AorusGrants

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Spigot%20%7C%20Paper-gold.svg)
![Java](https://img.shields.io/badge/java-17%2B-orange.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

> **AorusGrants** es un plugin de gestión de rangos basado en GUI, moderno y potente, diseñado específicamente para funcionar sobre **LuckPerms**. Olvídate de los comandos complejos y gestiona a tu staff y usuarios con un flujo de trabajo rápido e intuitivo.

---

## ✨ Características Principales

* 🧩 **Flujo GUI de 5 pasos:** Un proceso guiado para otorgar o retirar rangos sin errores.
* 🔒 **Integración Nativa:** 100% basado en la API de **LuckPerms**. Sin sistemas de permisos paralelos.
* ⏳ **Rangos Temporales:** Soporte para acumuladores de tiempo (Días, Semanas, Meses).
* ⚙️ **Altamente Configurable:** Personaliza cada menú, ítem y mensaje mediante `menus.yml`.
* 📊 **Gestión de Sesiones:** Sistema de expiración automática para evitar cambios accidentales.
* ⚡ **Tab-Complete:** Soporte completo para `/ag <jugador>`.
* 🎨 **Estética Premium:** Prefijos, materiales y layouts totalmente editables.

---

## 📸 Vista Previa

> *¡Próximamente! Aquí puedes añadir capturas de pantalla de los menús para mostrar la interfaz.*
> ![Preview Placeholder](https://via.placeholder.com/800x400?text=Insert+GUI+Screenshots+Here)

---

## 📦 Requisitos Técnicos

Para asegurar el correcto funcionamiento, asegúrate de cumplir con:

| Requisito | Versión Mínima |
| :--- | :--- |
| **Software** | Paper o Spigot 1.20+ |
| **Java** | Java 17+ |
| **Dependencia** | LuckPerms 5.4+ |

---

## 🚀 Instalación y Uso

1.  Descarga el archivo `.jar` más reciente.
2.  Muévelo a la carpeta `/plugins/` de tu servidor.
3.  Asegúrate de tener **LuckPerms** instalado.
4.  Reinicia el servidor para generar los archivos de configuración.
5.  Configura tus rangos en:
    * `plugins/AorusGrants/config.yml`
    * `plugins/AorusGrants/menus.yml`

---

## 🛠️ Comandos y Permisos

| Comando | Permiso | Descripción |
| :--- | :--- | :--- |
| `/ag <player>` | `aorusgrants.use` | Abre la interfaz de gestión para el jugador. |
| `/ag reload` | `aorusgrants.admin` | Recarga todos los archivos de configuración. |

---

## 🧭 Flujo del Menú

La lógica de la interfaz sigue un camino lógico para evitar confusiones:

```mermaid
graph TD
    A[/ag player] --> B{MENÚ PRINCIPAL}
    B --> C[PROMOTE]
    B --> D[DEMOTE]
    B --> E[INFO]
    
    C --> C1[Seleccionar Rango] --> C2[Seleccionar Duración] --> C3[Confirmar] --> C4((EJECUTAR))
    D --> D1[Confirmar] --> D2((EJECUTAR))
    E --> E1[Ver todos los grupos]
