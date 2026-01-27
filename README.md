# YouTube Proxy API

**Replacement diretto delle API di Google YouTube con cache automatica su database H2.**

Sostituisci semplicemente:
- `https://www.googleapis.com/youtube/v3/*` → `http://localhost:8080/youtube/v3/*`

## ✨ Funzionalità

- **🔄 Replacement 1:1**: URL e parametri identici alle API ufficiali Google
- **⚡ Cache intelligente**: I risultati vengono memorizzati nel database H2 per 24 ore
- **📊 Estrazione statistiche**: Views, likes, commenti vengono salvati in campi dedicati
- **🔍 API di analytics**: Endpoint aggiuntivi per analisi dei dati cachati
- **🔐 Autenticazione interna**: Sistema di API-KEY per proteggere l'accesso alle API del proxy
- **🎯 Zero configurazione client**: Cambia solo l'URL base, tutto il resto rimane identico

## 📚 Documentazione

- **[README.md](README.md)** - Panoramica generale e quick start
- **[API-KEYS.md](API-KEYS.md)** - 🔐 Guida completa al sistema di autenticazione con API-KEY
- **[EXAMPLES.md](EXAMPLES.md)** - Esempi pratici di integrazione (JavaScript, Python, Java)
- **[HELP.md](HELP.md)** - Guida ai test e risoluzione problemi
- **[test-api.sh](test-api.sh)** - Script bash per testare le API

## 🚀 Come usare come Replacement

### Prima (API Google originali)
```bash
curl "https://www.googleapis.com/youtube/v3/search?part=snippet&q=spring+boot&key=YOUR_API_KEY"
curl "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics&id=VIDEO_ID&key=YOUR_API_KEY"
```

### Dopo (con questo proxy)
```bash
# Genera prima una API-KEY interna
curl -X POST "http://localhost:8080/api/keys/generate?description=MyApp"

# Usa l'API-KEY generata
curl -H "X-API-Key: ypx_..." "http://localhost:8080/youtube/v3/search?part=snippet&q=spring+boot"
curl -H "X-API-Key: ypx_..." "http://localhost:8080/youtube/v3/videos?part=snippet,statistics&id=VIDEO_ID"
```

**⚠️ Importante**: 
- Tutte le richieste agli endpoint `/youtube/*` e `/api/statistics/*` richiedono un'API-KEY interna
- L'API-KEY va fornita nell'header `X-API-Key` o come parametro `api_key`
- Il parametro `key` di YouTube (se presente) viene rimosso e NON usato per YouTube

### Integrazione con il codice esistente

Cambia solo la **base URL** nel tuo client:

**JavaScript/TypeScript:**
```javascript
// Prima
const BASE_URL = 'https://www.googleapis.com/youtube/v3';

// Dopo
const BASE_URL = 'http://localhost:8080/youtube/v3';
```

**Python:**
```python
# Prima
base_url = "https://www.googleapis.com/youtube/v3"

# Dopo
base_url = "http://localhost:8080/youtube/v3"
```

**Java:**
```java
// Prima
String baseUrl = "https://www.googleapis.com/youtube/v3";

// Dopo
String baseUrl = "http://localhost:8080/youtube/v3";
```

## Configurazione

1. Imposta la tua API key di YouTube come variabile d'ambiente:
```bash
export YOUTUBE_API_KEY="la-tua-api-key"
```

2. Avvia l'applicazione:
```bash
./mvnw spring-boot:run
```

L'applicazione sarà disponibile su `http://localhost:8080`

## 🔐 Autenticazione

### Generare una API-KEY

Prima di usare le API del proxy, devi generare un'API-KEY:

```bash
curl -X POST "http://localhost:8080/api/keys/generate?description=MyApplication&daysValid=365"
```

**Risposta:**
```json
{
  "key": "ypx_a1b2c3d4e5f6789...",
  "description": "MyApplication",
  "createdAt": "2026-01-27T10:00:00",
  "expiresAt": "2027-01-27T10:00:00",
  "isActive": true
}
```

### Usare l'API-KEY

Fornisci l'API-KEY in uno dei seguenti modi:

**1. Header HTTP (raccomandato):**
```bash
curl -H "X-API-Key: ypx_..." "http://localhost:8080/youtube/v3/search?part=snippet&q=java"
```

**2. Query Parameter:**
```bash
curl "http://localhost:8080/youtube/v3/search?api_key=ypx_...&part=snippet&q=java"
```

## Endpoints

### Gestione API-KEY

#### Generare una nuova API-KEY
**POST** `/api/keys/generate`
- `description` (opzionale): Descrizione della chiave
- `daysValid` (opzionale): Giorni di validità (default: mai scade)

#### Elencare tutte le API-KEY
**GET** `/api/keys`

#### Dettagli di una API-KEY
**GET** `/api/keys/{id}`

#### Disattivare una API-KEY
**PUT** `/api/keys/{id}/deactivate`

#### Riattivare una API-KEY
**PUT** `/api/keys/{id}/activate`

#### Eliminare una API-KEY
**DELETE** `/api/keys/{id}`

### API YouTube (Replacement Diretto)

⚠️ **Richiedono autenticazione con API-KEY**

Questi endpoint sono **identici al 100%** alle API di Google YouTube:

### 1. Search API

**GET** `/youtube/v3/search`

**Replacement diretto per**: `https://www.googleapis.com/youtube/v3/search`

Cerca video su YouTube. I risultati vengono cachati automaticamente nel database.

**Tutti i parametri delle API Google sono supportati:**
- `part`: snippet, id
- `q`: Query di ricerca
- `maxResults`: Numero massimo di risultati
- `order`: date, rating, relevance, title, videoCount, viewCount
- `type`: channel, playlist, video
- `videoDefinition`: any, high, standard
- `videoDuration`: any, long, medium, short
- `key`: (opzionale) Se non fornito, usa quello configurato

**Esempio:**
```bash
# Identico alle API Google, ma con cache automatica
curl "http://localhost:8080/youtube/v3/search?part=snippet&q=spring+boot&maxResults=10&type=video"
```

### 2. Videos API

**GET** `/youtube/v3/videos`

**Replacement diretto per**: `https://www.googleapis.com/youtube/v3/videos`

Ottiene dettagli di uno o più video. I risultati vengono cachati con estrazione automatica delle statistiche.

**Tutti i parametri delle API Google sono supportati:**
- `part`: contentDetails, id, snippet, statistics, status, etc.
- `id`: ID del video o lista di ID separati da virgola
- `key`: (opzionale) Se non fornito, usa quello configurato

**Esempio:**
```bash
# Identico alle API Google, ma con cache automatica e autenticazione
curl -H "X-API-Key: ypx_..." "http://localhost:8080/youtube/v3/videos?part=snippet,statistics&id=dQw4w9WgXcQ"
```

### API aggiuntive per Analytics

⚠️ **Richiedono autenticazione con API-KEY**

Le statistiche vengono estratte automaticamente dal JSON delle API YouTube e salvate in campi dedicati del database.

**GET** `/api/statistics/video/{videoId}`

Ottiene le statistiche di un video specifico dal database locale.

**Risposta:**
```json
{
  "videoId": "dQw4w9WgXcQ",
  "title": "Titolo del video",
  "channelTitle": "Nome del canale",
  "publishedAt": "2009-10-25T06:57:33Z",
  "duration": "PT3M33S",
  "thumbnailUrl": "https://...",
  "statistics": {
    "viewCount": 1000000,
    "likeCount": 50000,
    "commentCount": 10000,
    "favoriteCount": 0
  },
  "cached": true,
  "cachedAt": "2026-01-26T19:30:00",
  "expiresAt": "2026-01-27T19:30:00",
  "expired": false
}
```

**GET** `/api/statistics/videos`

Ottiene le statistiche di tutti i video cachati.

**GET** `/api/statistics/top-viewed?limit=10`

Ottiene i video più visti dalla cache (solo video non scaduti).

**GET** `/api/statistics/summary`

Ottiene un riepilogo generale delle statistiche:
```json
{
  "totalCachedVideos": 50,
  "validCachedVideos": 45,
  "expiredCachedVideos": 5,
  "totalViews": 5000000,
  "totalLikes": 250000,
  "totalComments": 50000
}
```

### 4. Health Check


## 🎯 Vantaggi del Replacement

1. **💰 Riduzione costi**: Le richieste duplicate vengono servite dalla cache
2. **⚡ Performance**: Risposta istantanea per dati cachati
3. **📊 Analytics**: Accesso diretto alle statistiche via SQL o REST API
4. **🔄 Zero modifiche client**: Cambia solo l'URL base
5. **🛡️ Resilienza**: Continua a funzionare anche se le API Google sono lente
6. **📈 Monitoraggio**: Log dettagliati di tutte le richieste (cache HIT/MISS)
**GET** `/youtube/v3/health`

Verifica lo stato del servizio.

**Esempio:**
```bash
curl http://localhost:8080/youtube/v3/health
```

## Database H2

La console H2 è disponibile su: `http://localhost:8080/h2-console`

**Credenziali:**
- JDBC URL: `jdbc:h2:mem:youtubedb`
- Username: `sa`
- Password: (vuoto)

## Tabelle del Database

### cached_search_results
- `id`: ID primario
- `query_key`: Hash MD5 dei parametri della query
- `response_json`: Risposta dell'API in formato JSON
- `created_at`: Data di creazione
- `expires_at`: Data di scadenza (24 ore dopo la creazione)

### cached_videos
- `id`: ID primario
- `video_id`: ID del video YouTube
- `response_json`: Risposta dell'API in formato JSON
- `title`: Titolo del video
- `description`: Descrizione del video
- `channel_id`: ID del canale
- `channel_title`: Nome del canale
- `thumbnail_url`: URL dell'immagine di anteprima
- `view_count`: Numero di visualizzazioni
- `like_count`: Numero di like
- `comment_count`: Numero di commenti
- `favorite_count`: Numero di preferiti
- `duration`: Durata del video (formato ISO 8601, es: PT15M33S)
- `published_at`: Data di pubblicazione
- `category`: Categoria del video
- `created_at`: Data di creazione nella cache
- `expires_at`: Data di scadenza (24 ore dopo la creazione)

## Logica di Cache

1. Quando arriva una richiesta, il sistema genera una chiave univoca basata sui parametri
2. Cerca nel database se esiste una risposta cached valida (non scaduta)
3. Se trovata, restituisce la risposta dal database (Cache HIT)
4. Se non trovata o scaduta, chiama l'API di YouTube (Cache MISS)
5. Salva la nuova risposta nel database con scadenza a 24 ore

## Tecnologie

- Spring Boot 4.0.2
- H2 Database (in-memory)
- Spring Data JPA
- Spring WebFlux (per chiamate HTTP reattive)
- Lombok
- Maven

## Build e Deploy

```bash
# Compila il progetto
./mvnw clean package

# Esegui i test
./mvnw test

# Avvia l'applicazione
./mvnw spring-boot:run
```
