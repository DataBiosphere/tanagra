
    SELECT
        year_of_birth      
    FROM
        ${ENT_person}      
    ORDER BY
        CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(),
        age,
        DAY) / 365.25) AS INT64) ASC
