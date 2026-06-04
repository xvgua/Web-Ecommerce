import { describe, it, expect, beforeEach, beforeAll } from 'vitest'
import { useSearchHistory, type SearchHistoryItem } from '../useSearchHistory'

const store = new Map<string, string>()

beforeAll(() => {
  globalThis.localStorage = {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => { store.set(key, value) },
    removeItem: (key: string) => { store.delete(key) },
    clear: () => { store.clear() },
    get length() { return store.size },
    key: (index: number) => [...store.keys()][index] ?? null,
  } as Storage
})

describe('useSearchHistory', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('should return empty array initially', () => {
    const { getAll } = useSearchHistory()
    expect(getAll()).toEqual([])
  })

  it('should add items to history', () => {
    const { add, getAll } = useSearchHistory()

    add('手机')
    add('电脑')

    const history = getAll()
    expect(history).toHaveLength(2)
    expect(history[0].keyword).toBe('电脑')
    expect(history[1].keyword).toBe('手机')
  })

  it('should deduplicate keywords and move to front', () => {
    const { add, getAll } = useSearchHistory()

    add('手机')
    add('电脑')
    add('手机')

    const history = getAll()
    expect(history).toHaveLength(2)
    expect(history[0].keyword).toBe('手机')
    expect(history[1].keyword).toBe('电脑')
  })

  it('should cap at 20 items', () => {
    const { add, getAll } = useSearchHistory()

    for (let i = 0; i < 25; i++) {
      add(`关键词${i}`)
    }

    expect(getAll()).toHaveLength(20)
    expect(getAll()[0].keyword).toBe('关键词24')
  })

  it('should remove specific keywords', () => {
    const { add, remove, getAll } = useSearchHistory()

    add('手机')
    add('电脑')
    add('耳机')

    remove(['手机', '耳机'])

    const history = getAll()
    expect(history).toHaveLength(1)
    expect(history[0].keyword).toBe('电脑')
  })

  it('should clear all history', () => {
    const { add, clear, getAll } = useSearchHistory()

    add('手机')
    add('电脑')

    clear()

    expect(getAll()).toEqual([])
  })

  it('should persist across calls', () => {
    const { add } = useSearchHistory()
    add('手机')
    add('电脑')

    const { getAll } = useSearchHistory()
    const history = getAll()
    expect(history).toHaveLength(2)
  })

  it('should include timestamp on each item', () => {
    const { add, getAll } = useSearchHistory()

    const before = Date.now()
    add('手机')
    const after = Date.now()

    const item = getAll()[0]
    expect(item.timestamp).toBeGreaterThanOrEqual(before)
    expect(item.timestamp).toBeLessThanOrEqual(after)
  })
})
