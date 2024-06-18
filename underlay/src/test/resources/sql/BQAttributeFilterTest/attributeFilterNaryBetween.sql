
    SELECT
        year_of_birth      
    FROM
        ${ENT_person}      
    WHERE
        CAST(FLOOR(TIMESTAMP_DIFF(@currentTimestamp2, age, DAY) / 365.25) AS INT64) BETWEEN @val0 AND @val1
