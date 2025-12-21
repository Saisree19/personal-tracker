## ADDED Requirements
### Requirement: Loading and error states for charts
The system SHALL provide clear loading indicators and error messages for report visualizations, and SHALL avoid rendering stale or empty charts when data is unavailable.

#### Scenario: No data or fetch error
- **WHEN** a report fetch returns no data or fails
- **THEN** the system SHALL show a no-data or error message, display a loader while retrying if applicable, and SHALL NOT render stale chart slices
