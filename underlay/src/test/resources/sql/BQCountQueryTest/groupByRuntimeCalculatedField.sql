
    SELECT
        COUNT(id) AS T_IDCT,
        CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), age, DAY) / 365.25) AS INT64) AS age      
    FROM
        ${ENT_person}      
    GROUP BY
        age      
    ORDER BY
        T_IDCT DESC
