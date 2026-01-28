#!/bin/bash

# Script per popolare la cache YouTube Proxy
# Legge query da file e le invia all'endpoint search

# Configurazione
API_BASE_URL="${API_URL:-http://localhost:8080}"
QUERIES_FILE="${1:-queries.txt}"
MAX_RESULTS=50
VIDEO_CATEGORY_ID=10
DELAY_SECONDS="${DELAY:-1}"

# Colori
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Verifica file
if [ ! -f "$QUERIES_FILE" ]; then
    echo -e "${RED}❌ File non trovato: $QUERIES_FILE${NC}"
    echo "Uso: $0 [file_queries.txt]"
    echo "Esempio: $0 queries.txt"
    exit 1
fi

echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}🚀 YouTube Proxy Cache Populator${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "📄 File:      $QUERIES_FILE"
echo -e "🌐 Endpoint:  $API_BASE_URL/youtube/v3/search"
echo -e "📊 Max:       $MAX_RESULTS risultati per query"
echo ""

# Contatori
total=0
success=0
failed=0
total_videos=0

# Leggi file
while IFS= read -r query || [ -n "$query" ]; do
    # Salta vuote e commenti
    [[ -z "$query" ]] || [[ "$query" =~ ^[[:space:]]*# ]] && continue
    
    total=$((total + 1))
    full_query="${query} music video"
    
    echo -e "${YELLOW}[$total] ${NC}$query"
    
    # URL encode
    encoded=$(printf %s "$full_query" | jq -sRr @uri)
    
    # Richiesta
    url="${API_BASE_URL}/youtube/v3/search?part=snippet&q=${encoded}&type=video&maxResults=${MAX_RESULTS}&videoCategoryId=${VIDEO_CATEGORY_ID}&pageToken="
    
    response=$(curl -s -w "\n%{http_code}" -H "Origin: http://localhost:3000" "$url")
    http_code=$(tail -n1 <<< "$response")
    body=$(sed '$ d' <<< "$response")
    
    if [ "$http_code" -eq 200 ]; then
        count=$(jq -r '.items | length' <<< "$body" 2>/dev/null || echo "0")
        total_videos=$((total_videos + count))
        success=$((success + 1))
        echo -e "   ${GREEN}✅ $count video cached${NC}"
    else
        failed=$((failed + 1))
        error=$(jq -r '.error.message // "Errore sconosciuto"' <<< "$body" 2>/dev/null)
        echo -e "   ${RED}❌ HTTP $http_code - $error${NC}"
    fi
    
    sleep $DELAY_SECONDS
    
done < "$QUERIES_FILE"

# Riepilogo
echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}📊 Riepilogo${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "Totale query:    $total"
echo -e "${GREEN}✅ Successi:     $success${NC}"
echo -e "${RED}❌ Fallite:      $failed${NC}"
echo -e "${YELLOW}📹 Video cached: $total_videos${NC}"
echo ""
echo -e "🔍 Statistiche: ${API_BASE_URL}/api/statistics"
echo -e "💾 Database:    ${API_BASE_URL}/h2-console"
