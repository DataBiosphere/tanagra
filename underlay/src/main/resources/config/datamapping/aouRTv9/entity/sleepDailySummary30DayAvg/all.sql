SELECT
    row_id,
    person_id,
    sleep_log_id,
    sleep_date,
    thirty_day_avg_minutes_restless,
    thirty_day_avg_minutes_deep,
    thirty_day_avg_minutes_light,
    thirty_day_avg_minutes_rem,
    thirty_day_avg_minutes_wake
FROM `${omopDataset}.sleep_daily_summary_30dayavg`
