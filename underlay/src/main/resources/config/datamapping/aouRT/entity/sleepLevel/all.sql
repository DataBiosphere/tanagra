SELECT
    ROW_NUMBER() OVER() AS row_id,
    person_id,
    sleep_date,
    is_main_sleep,
    level,
    duration_in_min
FROM `${omopDataset}.sleep_level`
