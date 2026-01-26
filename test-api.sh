#!/bin/bash

# YouTube Proxy API - Test Script
# Replacement diretto delle API Google YouTube con cache automatica
# 
# Configurazione:
# export YOUTUBE_API_KEY="la-tua-api-key"
# ./mvnw spring-boot:run

BASE_URL="http://localhost:8080/youtube/v3"

echo "========================================="
echo "YouTube Proxy API - Test Script"
echo "Replacement diretto delle API Google"
echo "========================================="

# Test 1: Health Check
echo -e "\n1️⃣  Health Check"
curl -s "$BASE_URL/health" | jq .

# Test 2: Search API - Prima chiamata (API Google)
echo -e "\n2️⃣  Search API - Prima chiamata (cache MISS)"
echo "   Questa chiamata viene inoltrata alle API Google"
curl -s "$BASE_URL/search?part=snippet&q=spring+boot&maxResults=5&type=video" | jq '.items[0].snippet.title'

# Test 3: Search API - Seconda chiamata (Cache)
echo -e "\n3️⃣  Search API - Seconda chiamata (cache HIT)"
echo "   Questa chiamata viene servita dalla cache H2"
curl -s "$BASE_URL/search?part=snippet&q=spring+boot&maxResults=5&type=video" | jq '.items[0].snippet.title'

# Test 4: Videos API - Prima chiamata (API Google)
echo -e "\n4️⃣  Videos API - Dettagli video (cache MISS)"
# Usa un ID video valido
VIDEO_ID="dQw4w9WgXcQ"
curl -s "$BASE_URL/videos?part=snippet,statistics&id=$VIDEO_ID" | jq '.items[0] | {title: .snippet.title, views: .statistics.viewCount}'

# Test 5: Videos API - Seconda chiamata (Cache)
echo -e "\n5️⃣  Videos API - Seconda chiamata (cache HIT)"
curl -s "$BASE_URL/videos?part=snippet,statistics&id=$VIDEO_ID" | jq '.items[0].snippet.title'

# Test 6: Statistics API - Statistiche estratte
echo -e "\n6️⃣  Statistics API - Dati strutturati dal DB"
curl -s "http://localhost:8080/api/statistics/video/$VIDEO_ID" | jq '{title, views: .statistics.viewCount, likes: .statistics.likeCount, cached}'

# Test 7: Search con filtri avanzati
echo -e "\n7️⃣  Search con parametri avanzati"
curl -s "$BASE_URL/search?part=snippet&q=java&maxResults=3&order=viewCount&type=video&videoDuration=medium" | jq '.items[].snippet.title'

# Test 8: Top video più visti
echo -e "\n8️⃣  Top video più visti dalla cache"
curl -s "http://localhost:8080/api/statistics/top-viewed?limit=5" | jq '.[].title'

# Test 9: Riepilogo statistiche
echo -e "\n9️⃣  Riepilogo statistiche aggregate"
curl -s "http://localhost:8080/api/statistics/summary" | jq '.'

echo -e "\n========================================="
echo "✅ Test completati!"
echo "========================================="
echo ""
echo "💡 Suggerimenti:"
echo "   - Apri H2 Console: http://localhost:8080/h2-console"
echo "   - JDBC URL: jdbc:h2:mem:youtubedb"
echo "   - Controlla i log per vedere cache HIT/MISS"
echo ""
