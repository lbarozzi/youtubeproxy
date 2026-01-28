# Ricostruzione Risposte da Cache - Opzione 2

## 📋 Panoramica

Con l'implementazione dell'**Opzione 2**, il proxy YouTube è ora in grado di ricostruire risposte di ricerca parziali utilizzando i dati salvati nel database, anche quando la cache è scaduta e l'API key non è configurata.

## 🔧 Modifiche Implementate

### 1. Estensione Entità `CachedSearchResult`

Aggiunti nuovi campi per salvare metadati delle ricerche:

```java
// Metadati della ricerca
private String query;           // Parametro 'q' della ricerca
private String orderBy;         // Parametro 'order'
private String videoType;       // Parametro 'type' (video, channel, playlist)
private Integer maxResults;     // Numero di risultati richiesti
private String nextPageToken;   // Token per pagina successiva
private String prevPageToken;   // Token per pagina precedente
private Integer totalResults;   // Totale risultati disponibili
private String regionCode;      // Codice regione (es. IT, US)
```

### 2. Migrazione Database

Script SQL creato: [`db/migration/V2__add_search_metadata.sql`](src/main/resources/db/migration/V2__add_search_metadata.sql)

- Aggiunge le nuove colonne alla tabella `cached_search_results`
- Crea indici per ottimizzare le query (`idx_search_query`, `idx_search_expired`)

### 3. Nuovi Metodi nel Service

#### `extractAndSaveSearchMetadata()`
Estrae metadati dalla risposta JSON di YouTube e li salva nell'entità `CachedSearchResult`:
- Parametri della query (q, order, type, maxResults)
- Metadati dalla risposta (pageTokens, regionCode, totalResults)

#### `reconstructSearchResponse()`
Ricostruisce una risposta JSON compatibile con YouTube API dai video salvati nel database:
1. Cerca video nel DB che corrispondono alla query
2. Crea un JSON strutturato come le risposte YouTube
3. Popola items con i video trovati
4. Aggiunge metadati di base (pageInfo)

#### `findVideosMatchingQuery()`
Cerca video nel database che contengono la query in:
- Titolo
- Descrizione
- Nome canale

Ordina i risultati per data di pubblicazione (più recenti prima).

#### `countVideosInResponse()`
Conta i video in una risposta JSON (utilizzato per logging).

## 🔄 Flusso di Lavoro

### Scenario 1: Cache Valida ✅
```
Query → Cache DB → Dati Freschi → Risposta Completa
```
**Output:** Risposta originale di YouTube (completa di tutti i metadati)

### Scenario 2: Cache Miss + API Key Configurata ✅
```
Query → Cache MISS → YouTube API → Salva Cache + Metadati → Risposta Completa
```
**Output:** Nuova risposta da YouTube (salvata per riutilizzo futuro)

### Scenario 3: Cache Scaduta + NO API Key ⚠️
```
Query → Cache Scaduta → Restituisce Dati Scaduti
```
**Output:** Dati dalla cache scaduta (meglio di niente)

### Scenario 4: Cache MISS + NO API Key + Video nel DB 🆕
```
Query → Cache MISS → Ricerca Video DB → Ricostruzione Parziale
```
**Output:** Risposta ricostruita dai video salvati (PARZIALE)

### Scenario 5: Cache MISS + NO API Key + Nessun Video ❌
```
Query → Cache MISS → Nessun Video DB → IllegalStateException
```
**Output:** Errore (nessun dato disponibile)

## ⚠️ Limitazioni della Ricostruzione

La risposta ricostruita è **PARZIALE** e presenta alcune limitazioni:

### ❌ NON Disponibili
- **Pagination tokens** reali (nextPageToken, prevPageToken)
- **totalResults** accurato (solo conteggio locale)
- **Ranking originale** di YouTube (usa ordinamento per data)
- **Metadata completi** (etag, regionCode limitati)

### ✅ Disponibili
- **Video items** con:
  - ID video
  - Snippet (title, description, channelId, channelTitle, publishedAt)
  - Thumbnails
- **PageInfo** con conteggio locale
- **Struttura JSON** compatibile con client YouTube API

## 📊 Log ed Osservabilità

Il sistema genera log informativi per monitorare il comportamento:

```log
INFO  - Cache HIT per search query: abc123...
INFO  - Cache MISS per search query: def456..., chiamata API YouTube
WARN  - API key non configurata. Modalità solo database attiva.
INFO  - Tentativo di ricostruzione risposta dai video nel database per query: abc123
INFO  - Risposta ricostruita con successo da 5 video nel database
```

## 🧪 Testing

Per testare la ricostruzione:

1. **Popola il database** chiamando alcune query con API key configurata:
   ```bash
   curl "http://localhost:8080/youtube/search?q=spring+boot&maxResults=10&part=snippet"
   ```

2. **Rimuovi o invalida l'API key** in `application.properties`:
   ```properties
   youtube.api.key=${YOUTUBE_API_KEY}
   ```

3. **Richiedi una query simile**:
   ```bash
   curl "http://localhost:8080/youtube/search?q=spring&maxResults=5&part=snippet"
   ```

4. **Verifica la risposta**:
   - Se ci sono video nel DB che matchano "spring", riceverai una risposta ricostruita
   - Controlla i log per vedere: `"Risposta ricostruita con N video dal database"`

## 🎯 Vantaggi

1. ✅ **Degrado graduale**: Il servizio non fallisce completamente senza API key
2. ✅ **Riutilizzo dati**: Sfrutta tutti i video salvati nelle ricerche precedenti
3. ✅ **Compatibilità**: Le risposte ricostruite seguono lo schema YouTube API
4. ✅ **Ricerca flessibile**: Cerca nei titoli, descrizioni e canali
5. ✅ **Metadati persistenti**: Salva informazioni utili per ricostruzioni future

## 🚀 Prossimi Miglioramenti Possibili

- [ ] Migliorare l'algoritmo di matching (tokenizzazione, fuzzy search)
- [ ] Implementare ranking dei risultati basato su view count / like count
- [ ] Supportare ordinamenti personalizzati (order=date, order=viewCount)
- [ ] Aggiungere indici full-text per ricerche più veloci
- [ ] Implementare cache multi-livello (Redis + PostgreSQL)
- [ ] Aggiungere metriche per monitorare cache hit rate

## 📝 Note

- La ricostruzione è un **fallback di emergenza**, non un sostituto dell'API reale
- Per risultati ottimali, configurare sempre una API key valida
- I video vengono salvati automaticamente da tutte le ricerche con API key
- La cache ha durata di 24 ore (configurabile in `CachedSearchResult.onCreate()`)
