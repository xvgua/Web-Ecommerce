import { ref, onUnmounted, type Ref } from 'vue'

export interface StickyTab {
  id: string
  label: string
}

export function useStickyTabs(tabs: StickyTab[], navRef: Ref<HTMLElement | null>) {
  const activeTab = ref(tabs[0]?.id ?? '')

  function scrollToSection(id: string) {
    const el = document.getElementById(id)
    if (!el) return
    const navHeight = navRef.value?.offsetHeight ?? 48
    const top = el.getBoundingClientRect().top + window.scrollY - navHeight - 8
    window.scrollTo({ top, behavior: 'smooth' })
  }

  let observer: IntersectionObserver | null = null

  function setupObserver() {
    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            activeTab.value = entry.target.id
          }
        }
      },
      { rootMargin: '-80px 0px -60% 0px', threshold: 0 },
    )
    for (const tab of tabs) {
      const el = document.getElementById(tab.id)
      if (el) observer.observe(el)
    }
  }

  function destroyObserver() {
    observer?.disconnect()
  }

  onUnmounted(destroyObserver)

  return { activeTab, scrollToSection, setupObserver, destroyObserver }
}
