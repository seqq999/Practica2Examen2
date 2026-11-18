# Práctica Preparatoria 2 — Parte 1: Teoría y Teoría Aplicada

## Eje A — Diseño adaptativo en Flutter

### 1.1 Error `RenderFlex overflowed`

**1. Causa raíz (matemática).**
`Row` mide a sus hijos *no flexibles* con restricción horizontal **ilimitada** (`maxWidth = ∞`): cada hijo toma su ancho intrínseco. Luego el Row necesita:

```
W_requerido = W_icono(40) + W_column(ancho intrínseco del Text más largo) + W_saldo
```

y el ancho disponible es `W_disp = ancho de pantalla − padding/márgenes del Card`.
Con un nombre de 35 caracteres a 18 px, el `Column` crece tanto que `W_requerido > W_disp`. Flutter pinta las franjas amarillas/negras e informa la diferencia:
`overflow = W_requerido − W_disp = 48.7 px`. Ningún hijo puede encogerse porque ninguno es flexible (`flex = 0`).

**2. Corrección con flexión.** Envolver el `Column` en **`Expanded`** (equivale a `Flexible(fit: FlexFit.tight)`). El Row primero mide los hijos fijos (ícono y saldo) y entrega al `Column` exactamente el espacio sobrante: `W_column = W_disp − W_icono − W_saldo`. Así el saldo nunca sale de la pantalla.

**3. Elipsis.**

```dart
Expanded(
  child: Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(
        nombreCliente,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        softWrap: false,
        style: const TextStyle(fontSize: 18),
      ),
      Text('IBAN: $iban',
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontSize: 13)),
    ],
  ),
),
```

La propiedad clave es `overflow: TextOverflow.ellipsis` (junto con `maxLines: 1`).

### 1.2 `MediaQuery` vs `LayoutBuilder`

**1. Diferencia de alcance.** `MediaQuery.of(context).size` devuelve el tamaño **global** de la ventana/pantalla (viewport lógico del dispositivo), sin importar dónde esté el widget. `LayoutBuilder` entrega los **`BoxConstraints` locales** que el **padre inmediato** impone a ese nodo del árbol (minWidth/maxWidth/minHeight/maxHeight), es decir, el espacio real del que dispone el widget.

**2. Caso Master‑Detail.** En una tableta de 1200 px con un panel lateral de 350 px, `MediaQuery` sigue diciendo 1200 px. Un componente del panel que decida su diseño con `MediaQuery` creería que tiene espacio de "pantalla ancha" (p. ej. mostraría 3 columnas) y desbordaría dentro de 350 px. Además, no se vuelve a evaluar si cambia el tamaño del panel (divisor arrastrable) y el componente deja de ser reutilizable. Con `LayoutBuilder` el componente responde al ancho real de su contenedor (`constraints.maxWidth < 600 ? lista : grid`).

---

## Eje B — Seguridad OWASP

### 1.3 IDOR / BOLA

**1. Definición.** IDOR (*Insecure Direct Object Reference*) ocurre cuando la aplicación usa un identificador controlado por el usuario (`/api/cuentas/{id}/saldo`) para acceder directamente a un objeto del modelo **sin verificar que el solicitante tenga permiso sobre ese objeto concreto**. En APIs se le llama BOLA (*Broken Object Level Authorization*, OWASP API1). OWASP lo ubica en **A01:2021 – Broken Access Control** porque es una falla de *control de acceso*: el sistema no hace cumplir la política de "quién puede hacer qué sobre qué recurso". A01 subió al puesto #1 en 2021 porque fue la categoría con mayor incidencia en las aplicaciones analizadas (≈94 % con alguna forma de falla) y por su impacto directo en confidencialidad e integridad.

**2. Autenticación ≠ Autorización.** El JWT válido solo prueba la **autenticación**: "quien envía esto es `carlos_mora` y el token no fue alterado". No dice nada sobre **autorización a nivel de objeto**: "¿la cuenta 3 pertenece a `carlos_mora`?". Si el endpoint solo valida firma/expiración y luego hace `findById(id)`, cualquier cliente autenticado legítimo puede cambiar el `{id}` y leer cuentas ajenas; el token, de hecho, es lo que le *abre la puerta*. La defensa es comparar el `sub` del JWT con el propietario del recurso (o un rol privilegiado) **en el servidor**, en cada petición, y responder 403 si no coincide.

**3. Horizontal vs vertical.** Es **escalamiento horizontal**: un usuario con rol `CLIENTE` accede a recursos de otro usuario con el **mismo** rol/nivel (`maria_rojas`). Sería vertical si obtuviera funciones de un rol superior (p. ej. un cliente invocando endpoints de `ADMIN`).

### 1.4 OWASP Mobile M3 — Comunicaciones inseguras

**1. MitM.** Un *Man‑in‑the‑Middle* se interpone entre el teléfono y el servidor. En una Wi‑Fi pública el atacante usa ARP spoofing, un *Evil Twin* (AP falso con el mismo SSID) o DNS spoofing para que el tráfico pase por su máquina. Como `http://` viaja en **texto plano**, con un sniffer (Wireshark, mitmproxy, Bettercap) lee la cabecera `Authorization: Bearer <JWT>`, números de cuenta y saldos, y puede además **modificar** respuestas o reutilizar el token (secuestro de sesión) hasta que expire.

**2. Defensas nativas.**
- **Android** — `res/xml/network_security_config.xml`:
  ```xml
  <network-security-config>
      <base-config cleartextTrafficPermitted="false" />
  </network-security-config>
  ```
  referenciado en el `AndroidManifest.xml` con `android:networkSecurityConfig="@xml/network_security_config"` (y/o `android:usesCleartextTraffic="false"`). Desde Android 9 (API 28) el valor por defecto ya es `false`.
- **iOS** — `Info.plist`, App Transport Security:
  ```xml
  <key>NSAppTransportSecurity</key>
  <dict>
      <key>NSAllowsArbitraryLoads</key>
      <false/>
  </dict>
  ```
  sin `NSExceptionDomains` que permitan HTTP.

**3. Certificate / SSL Pinning.** La app incrusta el certificado del servidor o el hash de su **clave pública** (SPKI) y, durante el handshake TLS, rechaza cualquier certificado que no coincida, **aunque esté firmado por una CA de confianza del sistema**. En Flutter se hace con un `SecurityContext` que solo confía en el certificado empaquetado o con `HttpClient.badCertificateCallback`/paquetes como `http_certificate_pinning` o un interceptor de Dio que compara el fingerprint SHA‑256. Protege contra: una **CA raíz maliciosa instalada** en el dispositivo (por malware, MDM hostil o un proxy corporativo/ataque como mitmproxy), una CA comprometida que emita un certificado fraudulento para el dominio y proxies de inspección TLS.

---

## Eje C — Nginx como Reverse Proxy

### 1.5

**1. `proxy_pass`.** Nginx escucha en el puerto 80 para `api.insecurepay.cr`; al recibir la petición del cliente, abre una conexión **nueva** hacia `http://127.0.0.1:8080` (Spring Boot), le reenvía método, URI y cuerpo (añadiendo las cabeceras `Host`, `X-Real-IP`, `X-Forwarded-For` y `X-Forwarded-Proto`), espera la respuesta y la devuelve al cliente. El cliente nunca habla directamente con Spring Boot ni conoce su puerto.

**2. Sin `X-Forwarded-For`.** Para Spring Boot el par TCP siempre sería Nginx, por lo que `request.getRemoteAddr()` registraría **`127.0.0.1`** para todos los usuarios del mundo. Consecuencias: logs forenses inútiles (no se sabe quién atacó) y el rate‑limiting/bloqueo por IP trataría a todos como un único cliente (un atacante bloquearía a todos, o nunca se bloquearía a nadie). En Spring Boot además se debe habilitar `server.forward-headers-strategy=native` (o `framework`) para que confíe en estas cabeceras **solo** cuando vienen del proxy.

**3. Aislamiento del puerto 8080.** Si 8080 está abierto, un atacante puede **saltarse el proxy**: evita TLS (puerto 443), rate‑limiting, cabeceras de seguridad, WAF/reglas de Nginx, listas de bloqueo, y además puede falsificar `X-Forwarded-For` para envenenar logs. También expone directamente la superficie de Tomcat/Actuator/consola H2. Con `ufw` se aplica defensa en profundidad y mínima exposición:

```bash
sudo ufw default deny incoming
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw deny 8080/tcp
```

y/o configurar Spring Boot con `server.address=127.0.0.1` para que solo escuche en loopback.
