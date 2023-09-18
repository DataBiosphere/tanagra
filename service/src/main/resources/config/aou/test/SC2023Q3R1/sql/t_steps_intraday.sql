SELECT
    ROW_NUMBER() OVER() AS row_id,
    CAST(datetime AS DATE) as date,
    SUM(CAST(steps AS INT64)) as sum_steps,
    person_id
FROM `all-of-us-ehr-dev.SC2023Q3R1.steps_intraday`
GROUP BY 2, 4
