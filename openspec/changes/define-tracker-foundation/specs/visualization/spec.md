## ADDED Requirements
### Requirement: Interactive charts
The system SHALL display reports using interactive pie charts that respond to user interactions (hover, selection) and show which application consumed the most effort and complexity-wise distribution.

#### Scenario: Effort distribution rendered
- **WHEN** a user opens the reporting view with available data
- **THEN** the system SHALL render pie charts showing effort per application and complexity distribution, highlighting the segment representing the highest effort

### Requirement: Dynamic updates on filter changes
The system SHALL update charts dynamically when filters or sorting options change without requiring a full page reload.

#### Scenario: Charts refresh on filter change
- **WHEN** a user adjusts any report filter or sorting option
- **THEN** the system SHALL refresh the displayed charts and metrics to reflect the new criteria

### Requirement: No-data handling
The system SHALL communicate clearly when filtered data returns no results and SHALL avoid rendering misleading empty charts.

#### Scenario: No matching data
- **WHEN** filters yield no matching tasks
- **THEN** the system SHALL display a clear no-data message and SHALL NOT render stale chart data
