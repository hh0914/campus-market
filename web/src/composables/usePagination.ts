import { ref } from 'vue'

export function usePagination(initialSize = 12) {
  const page = ref(1)
  const size = ref(initialSize)
  const total = ref(0)

  function reset() {
    page.value = 1
  }

  function onSizeChange(next: number) {
    size.value = next
    page.value = 1
  }

  return { page, size, total, reset, onSizeChange }
}
