# Esempi di Utilizzo - YouTube Proxy API

## 🔄 Come Replacement Diretto delle API Google

Questo proxy è un **drop-in replacement** delle API di Google YouTube. Basta cambiare l'URL base.

### Prima: API Google dirette

```bash
# Search
curl "https://www.googleapis.com/youtube/v3/search?part=snippet&q=spring+boot&key=YOUR_KEY"

# Videos
curl "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics&id=VIDEO_ID&key=YOUR_KEY"
```

### Dopo: Con il proxy (identico)

```bash
# Search (stesso endpoint, stesso formato)
curl "http://localhost:8080/youtube/v3/search?part=snippet&q=spring+boot"

# Videos (stesso endpoint, stesso formato)
curl "http://localhost:8080/youtube/v3/videos?part=snippet,statistics&id=VIDEO_ID"
```

---

## 📋 Esempi Pratici

### 1. Cerca video per keyword

```bash
# Cerca "Python tutorial"
curl "http://localhost:8080/youtube/v3/search?part=snippet&q=python+tutorial&maxResults=5&type=video"

# Cerca con filtri avanzati
curl "http://localhost:8080/youtube/v3/search?part=snippet&q=java&maxResults=10&order=viewCount&type=video&videoDuration=medium"
```

### 2. Ottieni dettagli di un video

```bash
# Dettagli base
curl "http://localhost:8080/youtube/v3/videos?part=snippet&id=dQw4w9WgXcQ"

# Con statistiche
curl "http://localhost:8080/youtube/v3/videos?part=snippet,statistics&id=dQw4w9WgXcQ"

# Con tutto (snippet, statistics, contentDetails)
curl "http://localhost:8080/youtube/v3/videos?part=snippet,statistics,contentDetails&id=dQw4w9WgXcQ"
```

### 3. Multiple video IDs

```bash
# Ottieni dettagli di più video contemporaneamente
curl "http://localhost:8080/youtube/v3/videos?part=snippet,statistics&id=VIDEO_ID1,VIDEO_ID2,VIDEO_ID3"
```

---

## 💻 Integrazione nel Codice

### JavaScript / TypeScript

```javascript
// Configurazione
const YOUTUBE_API_BASE = 'http://localhost:8080/youtube/v3';

// Funzione di ricerca
async function searchVideos(query, maxResults = 10) {
    const url = `${YOUTUBE_API_BASE}/search?part=snippet&q=${encodeURIComponent(query)}&maxResults=${maxResults}&type=video`;
    const response = await fetch(url);
    return response.json();
}

// Funzione per dettagli video
async function getVideoDetails(videoId) {
    const url = `${YOUTUBE_API_BASE}/videos?part=snippet,statistics&id=${videoId}`;
    const response = await fetch(url);
    return response.json();
}

// Utilizzo
const results = await searchVideos('spring boot', 5);
console.log(results.items);

const details = await getVideoDetails('dQw4w9WgXcQ');
console.log(details.items[0].statistics);
```

### Python

```python
import requests

# Configurazione
YOUTUBE_API_BASE = 'http://localhost:8080/youtube/v3'

def search_videos(query, max_results=10):
    """Cerca video su YouTube"""
    url = f"{YOUTUBE_API_BASE}/search"
    params = {
        'part': 'snippet',
        'q': query,
        'maxResults': max_results,
        'type': 'video'
    }
    response = requests.get(url, params=params)
    return response.json()

def get_video_details(video_id):
    """Ottieni dettagli di un video"""
    url = f"{YOUTUBE_API_BASE}/videos"
    params = {
        'part': 'snippet,statistics',
        'id': video_id
    }
    response = requests.get(url, params=params)
    return response.json()

# Utilizzo
results = search_videos('python tutorial', max_results=5)
for item in results['items']:
    print(f"Titolo: {item['snippet']['title']}")

details = get_video_details('dQw4w9WgXcQ')
stats = details['items'][0]['statistics']
print(f"Views: {stats['viewCount']}, Likes: {stats['likeCount']}")
```

### Java (Spring RestTemplate)

```java
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class YouTubeProxyClient {
    
    private static final String BASE_URL = "http://localhost:8080/youtube/v3";
    private final RestTemplate restTemplate = new RestTemplate();
    
    public String searchVideos(String query, int maxResults) {
        String url = UriComponentsBuilder
            .fromHttpUrl(BASE_URL + "/search")
            .queryParam("part", "snippet")
            .queryParam("q", query)
            .queryParam("maxResults", maxResults)
            .queryParam("type", "video")
            .toUriString();
        
        return restTemplate.getForObject(url, String.class);
    }
    
    public String getVideoDetails(String videoId) {
        String url = UriComponentsBuilder
            .fromHttpUrl(BASE_URL + "/videos")
            .queryParam("part", "snippet,statistics")
            .queryParam("id", videoId)
            .toUriString();
        
        return restTemplate.getForObject(url, String.class);
    }
}
```

---

## 📊 API Analytics (Bonus)

### Statistiche dal database

```bash
# Statistiche di un video specifico
curl "http://localhost:8080/api/statistics/video/dQw4w9WgXcQ" | jq

# Tutti i video cachati
curl "http://localhost:8080/api/statistics/videos" | jq

# Top 10 video più visti
curl "http://localhost:8080/api/statistics/top-viewed?limit=10" | jq

# Riepilogo generale
curl "http://localhost:8080/api/statistics/summary" | jq
```

---

## 🔍 Verifica Cache

### Osserva i log

Quando avvii l'applicazione con `./mvnw spring-boot:run`, vedrai nei log:

```
Cache MISS per search query: xxx, chiamata API YouTube
Cache HIT per search query: xxx
Cache MISS per video: dQw4w9WgXcQ, chiamata API YouTube  
Cache HIT per video: dQw4w9WgXcQ
Estratte statistiche per video dQw4w9WgXcQ: views=1000000, likes=50000
```

### H2 Console

1. Apri: http://localhost:8080/h2-console
2. JDBC URL: `jdbc:h2:mem:youtubedb`
3. Username: `sa`
4. Password: (vuoto)

```sql
-- Vedi tutti i video cachati
SELECT video_id, title, view_count, like_count, created_at, expires_at 
FROM cached_videos;

-- Vedi tutte le ricerche cachate
SELECT query_key, created_at, expires_at 
FROM cached_search_results;

-- Top video per views
SELECT video_id, title, view_count 
FROM cached_videos 
ORDER BY view_count DESC 
LIMIT 10;
```

---

## ⚙️ Configurazione Avanzata

### Modifica durata cache

In `application.properties`, puoi modificare la logica di scadenza modificando il codice nelle entità:

```java
// CachedVideo.java e CachedSearchResult.java
expiresAt = createdAt.plusHours(24); // 24 ore (default)
expiresAt = createdAt.plusMinutes(30); // 30 minuti
expiresAt = createdAt.plusDays(7); // 7 giorni
```

### Usa con Docker

```bash
# Build
docker build -t youtube-proxy .

# Run
docker run -p 8080:8080 -e YOUTUBE_API_KEY=your_key youtube-proxy
```

---

## 🎯 Best Practices

1. **Usa sempre parametri identici alle API Google** per massima compatibilità
2. **Monitora i log** per vedere hit rate della cache
3. **Configura la cache duration** in base alle tue esigenze
4. **Usa le API analytics** per reports e statistiche aggregate
5. **Testa prima con poche richieste** per verificare il funzionamento

---

## 🐛 Troubleshooting

### Errore "Required parameter 'part' is missing"
Le API Google lo richiedono. Aggiungi sempre `part=snippet` o altro.

### Cache non funziona
Controlla i log per vedere se ci sono errori nel salvataggio su H2.

### API key non configurata
Imposta la variabile d'ambiente: `export YOUTUBE_API_KEY="your_key"`
