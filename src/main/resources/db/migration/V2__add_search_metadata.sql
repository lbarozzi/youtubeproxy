-- Aggiungi colonne per salvare metadati della ricerca
-- Questo permette la ricostruzione di risposte parziali dai dati nel DB

ALTER TABLE cached_search_results ADD COLUMN IF NOT EXISTS query VARCHAR(500);
ALTER TABLE cached_search_results ADD COLUMN IF NOT EXISTS order_by VARCHAR(50);
ALTER TABLE cached_search_results ADD COLUMN IF NOT EXISTS video_type VARCHAR(50);
ALTER TABLE cached_search_results ADD COLUMN IF NOT EXISTS max_results INTEGER;
ALTER TABLE cached_search_results ADD COLUMN IF NOT EXISTS next_page_token VARCHAR(255);
ALTER TABLE cached_search_results ADD COLUMN IF NOT EXISTS prev_page_token VARCHAR(255);
ALTER TABLE cached_search_results ADD COLUMN IF NOT EXISTS total_results INTEGER;
ALTER TABLE cached_search_results ADD COLUMN IF NOT EXISTS region_code VARCHAR(10);

-- Crea indici per ottimizzare le query di ricerca
CREATE INDEX IF NOT EXISTS idx_search_query ON cached_search_results(query);
CREATE INDEX IF NOT EXISTS idx_search_expired ON cached_search_results(expires_at);
