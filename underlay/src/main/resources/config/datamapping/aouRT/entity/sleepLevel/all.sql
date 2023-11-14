SELECT
    ROW_NUMBER() OVER() AS row_id,
    person_id,
    sleep_date,
    is_main_sleep,
    level,
    timestamp(start_datetime) as start_datetime_utc,
    format_datetime('%Y-%m-%dT%H:%M:%E*S', start_datetime) as start_datetime_str,
    duration_in_min
FROM `${omopDataset}.sleep_level`
