
    SELECT
        COUNT(id) AS T_CTDT,
        CAST(FLOOR(TIMESTAMP_DIFF(@currentTimestamp0, age, DAY) / 365.25) AS INT64) AS age      
    FROM
        ${ENT_person}      
    GROUP BY
        age      
    ORDER BY
        T_CTDT DESC
