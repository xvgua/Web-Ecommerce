export interface SearchHistoryItem {
  keyword: string
  timestamp: number
}

const STORAGE_KEY = 'search_history'
const MAX_ITEMS = 20

function load(): SearchHistoryItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function save(items: SearchHistoryItem[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
  } catch {
    // localStorage full or unavailable — silently ignore
  }
}

export function useSearchHistory() {
  function getAll(): SearchHistoryItem[] {
    return load()
  }

  function add(keyword: string): void {
    const items = load()
    const filtered = items.filter(item => item.keyword !== keyword)
    filtered.unshift({ keyword, timestamp: Date.now() })
    save(filtered.slice(0, MAX_ITEMS))
  }

  function remove(keywords: string[]): void {
    const items = load()
    save(items.filter(item => !keywords.includes(item.keyword)))
  }

  function clear(): void {
    localStorage.removeItem(STORAGE_KEY)
  }

  return { getAll, add, remove, clear }
}
