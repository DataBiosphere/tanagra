SELECT
    ROW_NUMBER() OVER() AS row_id,
    CAST(datetime AS DATE) as date,
    SUM(CAST(steps AS INT64)) as sum_steps,
    person_id
FROM `${omopDataset}.steps_intraday`
GROUP BY 2, 4
