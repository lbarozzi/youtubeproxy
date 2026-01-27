#!/bin/bash

# YouTube Proxy API - Test Script
# Replacement diretto delle API Google YouTube con cache automatica e autenticazione
# 
# Configurazione:
# 1. Avvia l'applicazione: ./mvnw spring-boot:run
# 2. Genera una API-KEY: curl -X POST "http://localhost:8080/api/keys/generate?description=Test"
# 3. Imposta la variabile: export PROXY_API_KEY="ypx_..."
# 4. Esegui questo script: ./test-api.sh

BASE_URL="http://localhost:8080"

# Verifica se API_KEY è impostata
if [ -z "$PROXY_API_KEY" ]; then
    echo "⚠️  PROXY_API_KEY non impostata!"
    echo ""
    echo "Genero automaticamente una nuova API-KEY..."
    
    # Genera una nuova API-KEY
    RESPONSE=$(curl -s -X POST "$BASE_URL/api/keys/generate?description=TestScript&daysValid=7")
    PROXY_API_KEY=$(echo $RESPONSE | jq -r '.key')
    
    if [ "$PROXY_API_KEY" == "null" ] || [ -z "$PROXY_API_KEY" ]; then
        echo "❌ Errore nella generazione dell'API-KEY"
        echo "Risposta: $RESPONSE"
        exit 1
    fi
    
    echo "✅ API-KEY generata: $PROXY_API_KEY"
    echo "   Salva questa chiave per usi futuri:"
    echo "   export PROXY_API_KEY=\"$PROXY_API_KEY\""
    echo ""
fi

echo "========================================="
echo "YouTube Proxy API - Test Script"
echo "Replacement diretto delle API Google"
echo "========================================="
echo "API-KEY: ${PROXY_API_KEY:0:20}..."
echo ""

# Test 1: Test autenticazione fallita
echo "1️⃣  Test autenticazione fallita (senza API-KEY)"
curl -s "$BASE_URL/youtube/v3/search?part=snippet&q=test" | jq .

# Test 2: Search API - Prima chiamata (API Google)
echo -e "\n2️⃣  Search API - Prima chiamata (cache MISS)"
echo "   Questa chiamata viene inoltrata alle API Google"
curl -s -H "X-API-Key: $PROXY_API_KEY" "$BASE_URL/youtube/v3/search?part=snippet&q=spring+boot&maxResults=5&type=video" | jq '.items[0].snippet.title'

# Test 3: Search API - Seconda chiamata (Cache)
echo -e "\n3️⃣  Search API - Seconda chiamata (cache HIT)"
echo "   Questa chiamata viene servita dalla cache H2"
curl -s -H "X-API-Key: $PROXY_API_KEY" "$BASE_URL/youtube/v3/search?part=snippet&q=spring+boot&maxResults=5&type=video" | jq '.items[0].snippet.title'

# Test 4: Videos API - Prima chiamata (API Google)
echo -e "\n4️⃣  Videos API - Dettagli video (cache MISS)"
# Usa un ID video valido
VIDEO_ID="dQw4w9WgXcQ"
curl -s -H "X-API-Key: $PROXY_API_KEY" "$BASE_URL/youtube/v3/videos?part=snippet,statistics&id=$VIDEO_ID" | jq '.items[0] | {title: .snippet.title, views: .statistics.viewCount}'

# Test 5: Videos API - Seconda chiamata (Cache)
echo -e "\n5️⃣  Videos API - Seconda chiamata (cache HIT)"
curl -s -H "X-API-Key: $PROXY_API_KEY" "$BASE_URL/youtube/v3/videos?part=snippet,statistics&id=$VIDEO_ID" | jq '.items[0].snippet.title'

# Test 6: Statistics API - Statistiche estratte
echo -e "\n6️⃣  Statistics API - Dati strutturati dal DB"
curl -s -H "X-API-Key: $PROXY_API_KEY" "$BASE_URL/api/statistics/video/$VIDEO_ID" | jq '{title, viewCount, likeCount, commentCount}'

# Test 7: Top video più visti
echo -e "\n7️⃣  Top video più visti dalla cache"
curl -s -H "X-API-Key: $PROXY_API_KEY" "$BASE_URL/api/statistics/top-viewed?limit=5" | jq '.[].title'

# Test 8: Riepilogo statistiche
echo -e "\n8️⃣  Riepilogo statistiche aggregate"
curl -s -H "X-API-Key: $PROXY_API_KEY" "$BASE_URL/api/statistics/summary" | jq '.'

# Test 9: Gestione API-KEY
echo -e "\n9️⃣  Lista API-KEY esistenti"
curl -s "$BASE_URL/api/keys" | jq '.[] | {id, description, isActive, createdAt}'

echo -e "\n========================================="
echo "✅ Test completati!"
echo "========================================="
echo ""
echo "💡 Suggerimenti:"
echo "   - Apri H2 Console: http://localhost:8080/h2-console"
echo "   - JDBC URL: jdbc:h2:file:./data/youtubedb"
echo "   - Controlla i log per vedere cache HIT/MISS"
echo "   - La tua API-KEY: $PROXY_API_KEY"
echo ""
