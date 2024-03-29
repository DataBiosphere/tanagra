SELECT
    ROW_NUMBER() OVER() AS row_id,
    date,
    zone_name,
    min_heart_rate,
    max_heart_rate,
    minute_in_zone,
    calorie_count,
    person_id
FROM `${omopDataset}.heart_rate_summary`
