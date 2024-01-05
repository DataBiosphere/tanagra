
    SELECT
        CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(),
        age,
        DAY) / 365.25) AS INT64) AS age      
    FROM
        ${ENT_person}      
    ORDER BY
        age DESC
