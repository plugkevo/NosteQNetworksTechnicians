# LOS (Loss of Signal) Duration Feature

## Overview
This feature displays how long an ONU (Optical Network Unit) has been in LOS (Loss of Signal) status, showing human-readable durations like "12 hours ago", "1 day ago", etc.

## What Changed

### 1. **SmartOltApiService.kt**
- Added `calculateTimeSinceLosStatus()` utility function that:
  - Parses ONU timestamps in multiple formats (e.g., "2019-02-15 07:49:01+08:00")
  - Calculates time differences from now
  - Returns human-readable durations (minutes, hours, days, weeks)
  
- Enhanced `OnuFullStatus` data class with:
  - `lastDownTime: String?` - The timestamp when ONU went down
  - `losStatusDuration: String?` - Human-readable duration (e.g., "12 hours ago")

- Updated `parseFullStatusInfo()` function to:
  - Extract "Last down time" from API response
  - Detect LOS status based on "Last down cause" field
  - Automatically calculate LOS duration

### 2. **NetworkViewModel.kt**
- Added `_selectedOnuFullStatus` state flow to store full ONU status data
- Updated `fetchOnuFullStatus()` to store the response and log LOS duration

### 3. **RouterScreen.kt (UI)**
- Added collection of `fullStatus` from ViewModel
- Display LOS duration card when:
  - ONU is in LOS status
  - `losStatusDuration` is available
- Shows:
  - "Loss of Signal Duration" with the calculated time (e.g., "12 hours ago")
  - Last down cause (if available)

## How It Works

### API Response Parsing
When the `get_onu_full_status_info` endpoint returns data like:
```
Last down time: 2019-02-15 07:49:01+08:00
Last down cause: ONT LOSi/LOBi alarm
```

The system:
1. Extracts the timestamp and cause
2. Calculates time elapsed since that timestamp
3. Formats it as "X hours ago", "1 day ago", etc.

### Display Logic
In the ONU Details screen, if an ONU is in LOS status:
- A highlighted card appears showing the LOS duration
- Users immediately see how long the device has been offline
- Additional context is provided with the down cause

## Example Durations
- "Just now" - Within 1 minute
- "5 minutes ago" - Less than 1 hour
- "12 hours ago" - Less than 1 day  
- "2 days ago" - Less than 7 days
- "3 weeks ago" - 7+ days

## Testing
To test this feature:
1. Navigate to an ONU device that has LOS status
2. The device details screen will show the "Loss of Signal Duration" card
3. The duration will be calculated based on the "Last down time" in the API response

## Future Enhancements
- Periodic refresh of LOS duration display
- LOS duration history/timeline view
- Alerts when LOS duration exceeds threshold
- Auto-formatting based on locale/timezone
