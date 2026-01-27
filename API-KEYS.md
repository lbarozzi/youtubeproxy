# Gestione API-KEY Interne

## 🔐 Sistema di Autenticazione

Il YouTube Proxy utilizza un sistema di **API-KEY interne** per proteggere l'accesso alle API del proxy.

**⚠️ Importante**: Le API-KEY sono **interne al programma** e NON sono le chiavi YouTube. Non vengono mai inviate a YouTube.

## 🎯 Scopo

- **Proteggere** l'accesso alle API del proxy
- **Tracciare** l'utilizzo delle API
- **Controllare** chi può accedere ai dati cachati
- **Gestire** l'accesso in modo granulare

## 📝 Generare una API-KEY

### Endpoint: POST `/api/keys/generate`

**Parametri Query (opzionali):**
- `description`: Descrizione della chiave (es. "App Mobile", "Frontend Web")
- `daysValid`: Giorni di validità (default: mai scade)

**Esempio:**
```bash
# API-KEY senza scadenza
curl -X POST "http://localhost:8080/api/keys/generate?description=MyApp"

# API-KEY valida per 90 giorni
curl -X POST "http://localhost:8080/api/keys/generate?description=TestApp&daysValid=90"
```

**Risposta:**
```json
{
  "key": "ypx_a1b2c3d4e5f67890123456789abcdef0",
  "description": "MyApp",
  "createdAt": "2026-01-27T10:00:00",
  "expiresAt": null,
  "isActive": true
}
```

**⚠️ Salva la chiave!** Non potrai recuperarla successivamente.

## 🔑 Usare l'API-KEY

### Metodo 1: Header HTTP (raccomandato)

```bash
curl -H "X-API-Key: ypx_..." "http://localhost:8080/youtube/v3/search?part=snippet&q=java"
```

### Metodo 2: Query Parameter

```bash
curl "http://localhost:8080/youtube/v3/search?api_key=ypx_...&part=snippet&q=java"
```

## 📋 Gestione API-KEY

### Elencare tutte le API-KEY

```bash
GET /api/keys
```

**Esempio:**
```bash
curl "http://localhost:8080/api/keys" | jq
```

**Risposta:**
```json
[
  {
    "id": 1,
    "keyValue": "ypx_a1b2c3d4e5f67890123456789abcdef0",
    "description": "MyApp",
    "isActive": true,
    "createdAt": "2026-01-27T10:00:00",
    "expiresAt": null,
    "lastUsedAt": "2026-01-27T11:30:00"
  }
]
```

### Dettagli di una API-KEY

```bash
GET /api/keys/{id}
```

**Esempio:**
```bash
curl "http://localhost:8080/api/keys/1" | jq
```

### Disattivare una API-KEY

```bash
PUT /api/keys/{id}/deactivate
```

**Esempio:**
```bash
curl -X PUT "http://localhost:8080/api/keys/1/deactivate"
```

**Risposta:**
```json
{
  "message": "API-KEY disattivata con successo",
  "id": 1
}
```

### Riattivare una API-KEY

```bash
PUT /api/keys/{id}/activate
```

**Esempio:**
```bash
curl -X PUT "http://localhost:8080/api/keys/1/activate"
```

### Eliminare una API-KEY

```bash
DELETE /api/keys/{id}
```

**Esempio:**
```bash
curl -X DELETE "http://localhost:8080/api/keys/1"
```

**Risposta:**
```json
{
  "message": "API-KEY eliminata con successo",
  "id": 1
}
```

## 🛡️ Endpoint Protetti

Gli endpoint seguenti **richiedono** autenticazione con API-KEY:

### API YouTube Proxy
- `GET /youtube/v3/search`
- `GET /youtube/v3/videos`

### API Statistics
- `GET /api/statistics/video/{videoId}`
- `GET /api/statistics/videos`
- `GET /api/statistics/top-viewed`
- `GET /api/statistics/summary`

## 🔓 Endpoint Pubblici

Gli endpoint seguenti **NON richiedono** autenticazione:

- `POST /api/keys/generate` - Generazione API-KEY
- `GET /api/keys` - Lista API-KEY (può essere protetto in produzione)
- `GET /api/keys/{id}` - Dettagli API-KEY
- `PUT /api/keys/{id}/deactivate` - Gestione API-KEY
- `PUT /api/keys/{id}/activate` - Gestione API-KEY
- `DELETE /api/keys/{id}` - Gestione API-KEY
- `/h2-console` - Console H2 (solo sviluppo)

## 🚨 Errori di Autenticazione

### 401 Unauthorized - API-KEY mancante

```json
{
  "error": {
    "code": 401,
    "message": "API-KEY mancante. Usa header 'X-API-Key' o parametro 'api_key'",
    "status": "UNAUTHORIZED"
  }
}
```

### 401 Unauthorized - API-KEY non valida

```json
{
  "error": {
    "code": 401,
    "message": "API-KEY non valida o scaduta",
    "status": "UNAUTHORIZED"
  }
}
```

## 💡 Best Practices

### 1. Usa Header X-API-Key
Preferisci l'header HTTP rispetto al query parameter per maggiore sicurezza.

### 2. Descrizioni Significative
Assegna descrizioni chiare alle API-KEY per identificare facilmente le applicazioni.

```bash
curl -X POST "http://localhost:8080/api/keys/generate?description=Frontend_Production"
curl -X POST "http://localhost:8080/api/keys/generate?description=Mobile_App_iOS"
curl -X POST "http://localhost:8080/api/keys/generate?description=Analytics_Dashboard"
```

### 3. Imposta Scadenze
Per ambienti di test, imposta scadenze per le API-KEY.

```bash
curl -X POST "http://localhost:8080/api/keys/generate?description=Test_Environment&daysValid=30"
```

### 4. Disattiva invece di Eliminare
Se sospetti un uso improprio, disattiva la chiave invece di eliminarla per mantenere lo storico.

```bash
curl -X PUT "http://localhost:8080/api/keys/1/deactivate"
```

### 5. Monitora l'Utilizzo
Controlla `lastUsedAt` per identificare chiavi inutilizzate.

```bash
curl "http://localhost:8080/api/keys" | jq '.[] | {description, lastUsedAt, isActive}'
```

## 🔧 Configurazione Applicazione

Non c'è bisogno di configurare nulla per l'autenticazione interna. Il filtro è attivo automaticamente.

### application.properties

```properties
# Nessuna configurazione necessaria per le API-KEY interne
# Le chiavi YouTube (se configurate) sono separate
youtube.api.key=${YOUTUBE_API_KEY:}
```

## 📦 Database

Le API-KEY sono salvate nella tabella `api_keys` del database H2:

```sql
SELECT * FROM api_keys;
```

### Schema

```sql
CREATE TABLE api_keys (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    key_value VARCHAR(64) UNIQUE NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP
);
```

## 🧪 Test

Script bash per testare l'autenticazione:

```bash
#!/bin/bash

# Genera API-KEY
RESPONSE=$(curl -s -X POST "http://localhost:8080/api/keys/generate?description=Test")
API_KEY=$(echo $RESPONSE | jq -r '.key')
echo "API-KEY generata: $API_KEY"

# Test con API-KEY valida
echo "Test con API-KEY valida:"
curl -H "X-API-Key: $API_KEY" "http://localhost:8080/youtube/v3/search?part=snippet&q=test&maxResults=1"

# Test senza API-KEY (dovrebbe fallire)
echo "Test senza API-KEY (401 atteso):"
curl "http://localhost:8080/youtube/v3/search?part=snippet&q=test"

# Test con API-KEY errata (dovrebbe fallire)
echo "Test con API-KEY errata (401 atteso):"
curl -H "X-API-Key: invalid_key" "http://localhost:8080/youtube/v3/search?part=snippet&q=test"
```

## 🔄 Migrazione da Versioni Precedenti

Se hai già un'applicazione in esecuzione senza autenticazione:

1. **Avvia** la nuova versione
2. **Genera** API-KEY per le tue applicazioni client
3. **Aggiorna** i client per includere l'header `X-API-Key`
4. **Verifica** che tutto funzioni correttamente

### Esempio Migrazione Client JavaScript

**Prima:**
```javascript
fetch('http://localhost:8080/youtube/v3/search?part=snippet&q=java')
```

**Dopo:**
```javascript
const API_KEY = 'ypx_...'; // La tua API-KEY

fetch('http://localhost:8080/youtube/v3/search?part=snippet&q=java', {
    headers: {
        'X-API-Key': API_KEY
    }
})
```

## 📞 Supporto

Per problemi o domande sull'autenticazione:
1. Verifica che l'API-KEY sia valida e attiva
2. Controlla i log dell'applicazione per dettagli
3. Usa la console H2 per ispezionare lo stato delle chiavi
