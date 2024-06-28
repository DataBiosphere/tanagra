
    SELECT
        CAST(FLOOR(TIMESTAMP_DIFF(@currentTimestamp0, age, DAY) / 365.25) AS INT64) AS age      
    FROM
        ${ENT_person}      
    ORDER BY
        age DESC
