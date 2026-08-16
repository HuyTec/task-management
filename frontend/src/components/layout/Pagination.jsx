function Pagination({ page, onPageChange, label = 'items' }) {
  if (!page || page.totalPages <= 1) return null

  const start = page.totalElements === 0 ? 0 : page.pageNumber * page.pageSize + 1
  const end = Math.min((page.pageNumber + 1) * page.pageSize, page.totalElements)

  return (
    <nav className="pagination" aria-label={`${label} pagination`}>
      <p className="pagination__summary">
        Showing {start}–{end} of {page.totalElements} {label}
      </p>
      <div className="pagination__actions">
        <button className="text-button" type="button" disabled={page.first} onClick={() => onPageChange(page.pageNumber - 1)}>Previous</button>
        <span>Page {page.pageNumber + 1} of {page.totalPages}</span>
        <button className="text-button" type="button" disabled={page.last} onClick={() => onPageChange(page.pageNumber + 1)}>Next</button>
      </div>
    </nav>
  )
}

export default Pagination
