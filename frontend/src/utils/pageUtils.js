export function decrementPageTotal(page) {
  if (!page) return page

  const totalElements = Math.max(0, page.totalElements - 1)
  const totalPages = totalElements === 0 ? 0 : Math.ceil(totalElements / page.pageSize)

  return {
    ...page,
    totalElements,
    totalPages,
    last: totalPages === 0 || page.pageNumber >= totalPages - 1,
  }
}
