/**
 * Client-side filtering for the Projects list page.
 * Filters projects by name, slug, description, or version in real-time.
 */
document.addEventListener('DOMContentLoaded', function() {
    const textFilter = document.getElementById('textFilter');
    const clearButton = document.getElementById('clearFilter');

    if (!textFilter) return;

    // Filter function
    function filterProjects() {
        const filterValue = textFilter.value.toLowerCase().trim();

        // Show/hide clear button
        if (filterValue) {
            clearButton.style.display = 'block';
        } else {
            clearButton.style.display = 'none';
        }

        // Get all project rows (desktop table) and cards (mobile)
        const tableRows = document.querySelectorAll('.table-hover tbody tr');
        const mobileCards = document.querySelectorAll('.d-md-none .card');

        let visibleCount = 0;

        // Filter desktop table rows
        tableRows.forEach(function(row) {
            const name = row.querySelector('td:nth-child(2)')?.textContent?.toLowerCase() || '';
            const slug = row.querySelector('td:nth-child(3)')?.textContent?.toLowerCase() || '';
            const description = row.querySelector('td:nth-child(4)')?.textContent?.toLowerCase() || '';
            const versions = row.querySelector('td:nth-child(5)')?.textContent?.toLowerCase() || '';

            const searchText = name + ' ' + slug + ' ' + description + ' ' + versions;

            if (filterValue === '' || searchText.includes(filterValue)) {
                row.classList.remove('project-row-hidden');
                visibleCount++;
            } else {
                row.classList.add('project-row-hidden');
            }
        });

        // Filter mobile cards
        mobileCards.forEach(function(card) {
            const name = card.querySelector('.card-title span')?.textContent?.toLowerCase() || '';
            const slug = card.querySelector('.card-text code')?.textContent?.toLowerCase() || '';
            const description = card.querySelector('.card-text:not(:has(code))')?.textContent?.toLowerCase() || '';
            const badges = Array.from(card.querySelectorAll('.badge'))
                .map(b => b.textContent.toLowerCase())
                .join(' ');

            const searchText = name + ' ' + slug + ' ' + description + ' ' + badges;

            if (filterValue === '' || searchText.includes(filterValue)) {
                card.classList.remove('project-card-hidden');
            } else {
                card.classList.add('project-card-hidden');
            }
        });

        // Update the project count in the summary
        const summaryElement = document.querySelector('.mt-3.text-muted small span');
        if (summaryElement && filterValue) {
            summaryElement.textContent = visibleCount;
        }
    }

    // Clear button handler
    clearButton.addEventListener('click', function() {
        textFilter.value = '';
        filterProjects();
        textFilter.focus();
    });

    // Real-time filtering as user types
    textFilter.addEventListener('input', filterProjects);

    // Also filter on keyup for better responsiveness
    textFilter.addEventListener('keyup', function(e) {
        // Clear on Escape key
        if (e.key === 'Escape') {
            textFilter.value = '';
            filterProjects();
        }
    });
});
