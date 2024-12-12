-- test data does not have row_id, compute it
SELECT
    ROW_NUMBER() OVER() AS row_id,
    person_id,
    date,
    zone_name,
    min_heart_rate,
    max_heart_rate,
    minute_in_zone,
    calorie_count
FROM `${omopDataset}.heart_rate_summary`
