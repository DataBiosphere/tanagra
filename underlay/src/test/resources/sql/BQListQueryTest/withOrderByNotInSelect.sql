
    SELECT
        year_of_birth      
    FROM
        ${ENT_person}      
    ORDER BY
        CAST(FLOOR(TIMESTAMP_DIFF(@currentTimestamp0,
        age,
        DAY) / 365.25) AS INT64) ASC
