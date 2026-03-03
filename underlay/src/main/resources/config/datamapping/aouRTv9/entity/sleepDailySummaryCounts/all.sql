SELECT
    row_id,
    person_id,
    sleep_log_id,
    sleep_date,
    count_restless,
    count_deep,
    count_light,
    count_rem,
    count_wake
FROM `${omopDataset}.sleep_daily_summary_counts`
