SELECT
    ROW_NUMBER() OVER() AS row_id,
    person_id,
    CAST(datetime AS DATE) as date,
    AVG(heart_rate_value) avg_rate
FROM `${omopDataset}.heart_rate_minute_level`
GROUP BY
2, 3
