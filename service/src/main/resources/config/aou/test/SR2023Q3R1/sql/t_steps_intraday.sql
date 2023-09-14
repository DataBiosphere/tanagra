SELECT
    ROW_NUMBER() OVER() AS row_id,
    timestamp(datetime) AS datetime_utc,
    format_datetime('%Y-%m-%dT%H:%M:%S', datetime) as datetime_str,
    cast(steps as int64) AS steps,
    person_id
FROM `all-of-us-ehr-dev.SR2023Q3R1.steps_intraday`
