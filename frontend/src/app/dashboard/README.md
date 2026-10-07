# QuickBooks Commerce Professional Dashboard — Independent Workspace Filters

Updated dashboard files so Overview, Sales, and Products maintain independent date/category filter state.

## UX behavior
- Overview has its own date range (defaults to the last 30 days).
- Sales has its own date range and category filter.
- Products has its own date range and category filter.
- Changing Sales filters does not change Overview.
- Changing Products filters does not change Overview or Sales.
- Daily Sales uses the date range from the page where the category was selected.
- Forecast and Analytics remain independent of Sales/Products filters.

## Files
- `dashboard.ts`
- `dashboard.html`
- `dashboard.css`
- `dashboard.spec.ts`
