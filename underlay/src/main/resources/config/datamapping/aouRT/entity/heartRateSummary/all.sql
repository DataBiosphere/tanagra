SELECT
    row_id,
    person_id,
    date,
    zone_name,
    min_heart_rate,
    max_heart_rate,
    minute_in_zone,
    calorie_count
FROM `${omopDataset}.heart_rate_summary`
