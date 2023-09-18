SELECT
    ROW_NUMBER() OVER() AS row_id,
    timestamp(datetime) AS datetime_utc,
    format_datetime('%Y-%m-%dT%H:%M:%S', datetime) as datetime_str,
    heart_rate_value,
    person_id
FROM `all-of-us-ehr-dev.SC2023Q3R1.heart_rate_minute_level`
