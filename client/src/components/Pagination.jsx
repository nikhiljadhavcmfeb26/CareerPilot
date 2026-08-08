const Pagination = ({ currentPage, totalPages, onPageChange }) => {
  if (totalPages <= 1) return null;

  const pages = [];
  for (let i = 1; i <= totalPages; i++) pages.push(i);

  return (
    <nav className="mt-4">
      <ul className="pagination justify-content-center">
        <li className={`page-item ${currentPage === 1 ? 'disabled' : ''}`}>
          <button className="page-link" onClick={() => onPageChange(currentPage - 1)}>Previous</button>
        </li>
        {pages.map((p) => (
          <li key={p} className={`page-item ${currentPage === p ? 'active' : ''}`}>
            <button className="page-link" onClick={() => onPageChange(p)}>{p}</button>
          </li>
        ))}
        <li className={`page-item ${currentPage === totalPages ? 'disabled' : ''}`}>
          <button className="page-link" onClick={() => onPageChange(currentPage + 1)}>Next</button>
        </li>
      </ul>
    </nav>
  );
};

export default Pagination;
