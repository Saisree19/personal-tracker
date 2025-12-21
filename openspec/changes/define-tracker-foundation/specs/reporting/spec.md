## ADDED Requirements
### Requirement: Time-based reporting filters
The system SHALL provide report filters for weekly, monthly, quarterly, half-yearly, and yearly time windows applied to task completion dates.

#### Scenario: Monthly filter applied
- **WHEN** a user selects the monthly filter for reports
- **THEN** the system SHALL return metrics calculated using tasks completed within the selected month

### Requirement: Attribute filtering and sorting
The system SHALL support filtering reports by application/project name and task complexity, and SHALL support sorting by complexity (ascending/descending) and completion date.

#### Scenario: Filter by application and complexity
- **WHEN** a user filters reports by a specific application and complexity
- **THEN** the system SHALL return only tasks matching those criteria and SHALL allow the user to sort by the chosen order

### Requirement: Report outputs
The system SHALL generate report outputs that include number of tasks completed per application, complexity distribution across applications, and productivity trends over time.

#### Scenario: Productivity trend calculation
- **WHEN** a user runs a report with any supported time filter
- **THEN** the system SHALL include trend data showing task completion counts over the selected time periods and per-application completion counts
