/**
 * Client-side filtering for the Versions list page.
 * Filters versions by project name, version number, or status in real-time.
 */
document.addEventListener('DOMContentLoaded', function() {
    const textFilter = document.getElementById('textFilter');
    const clearButton = document.getElementById('clearFilter');

    if (!textFilter) return;

    // Filter function
    function filterVersions() {
        const filterValue = textFilter.value.toLowerCase().trim();

        // Show/hide clear button
        if (filterValue) {
            clearButton.style.display = 'block';
        } else {
            clearButton.style.display = 'none';
        }

        // Get all version rows (desktop table) and cards (mobile)
        const tableRows = document.querySelectorAll('.table-hover tbody tr');
        const mobileCards = document.querySelectorAll('.d-md-none .card');

        let visibleCount = 0;

        // Filter desktop table rows
        tableRows.forEach(function(row) {
            const projectName = row.querySelector('td:nth-child(1)')?.textContent?.toLowerCase() || '';
            const version = row.querySelector('td:nth-child(2)')?.textContent?.toLowerCase() || '';
            const status = row.querySelector('td:nth-child(4)')?.textContent?.toLowerCase() || '';
            const ossSupportEnd = row.querySelector('td:nth-child(6)')?.textContent?.toLowerCase() || '';
            const enterpriseSupportEnd = row.querySelector('td:nth-child(7)')?.textContent?.toLowerCase() || '';

            const searchText = projectName + ' ' + version + ' ' + status + ' ' + ossSupportEnd + ' ' + enterpriseSupportEnd;

            if (filterValue === '' || searchText.includes(filterValue)) {
                row.classList.remove('version-row-hidden');
                visibleCount++;
            } else {
                row.classList.add('version-row-hidden');
            }
        });

        // Filter mobile cards
        mobileCards.forEach(function(card) {
            const projectName = card.querySelector('.card-title span')?.textContent?.toLowerCase() || '';
            const version = card.querySelector('.card-text code')?.textContent?.toLowerCase() || '';
            const status = card.querySelector('.badge')?.textContent?.toLowerCase() || '';

            const searchText = projectName + ' ' + version + ' ' + status;

            if (filterValue === '' || searchText.includes(filterValue)) {
                card.classList.remove('version-card-hidden');
            } else {
                card.classList.add('version-card-hidden');
            }
        });

        // Update the version count in the summary
        const summaryElement = document.getElementById('versionCount');
        if (summaryElement && filterValue) {
            summaryElement.textContent = visibleCount;
        }
    }

    // Clear button handler
    clearButton.addEventListener('click', function() {
        textFilter.value = '';
        filterVersions();
        textFilter.focus();
    });

    // Real-time filtering as user types
    textFilter.addEventListener('input', filterVersions);

    // Also filter on keyup for better responsiveness
    textFilter.addEventListener('keyup', function(e) {
        // Clear on Escape key
        if (e.key === 'Escape') {
            textFilter.value = '';
            filterVersions();
        }
    });
});
