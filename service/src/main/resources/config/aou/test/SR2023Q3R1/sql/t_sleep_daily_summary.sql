SELECT
    ROW_NUMBER() OVER() AS row_id,
    person_id,
    sleep_date,
    is_main_sleep,
    minute_in_bed,
    minute_asleep,
    minute_after_wakeup,
    minute_awake,
    minute_restless,
    minute_deep,
    minute_light,
    minute_rem,
    minute_wake
FROM `all-of-us-ehr-dev.SR2023Q3R1.sleep_daily_summary`
