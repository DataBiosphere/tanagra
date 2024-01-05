
    SELECT
        year_of_birth,
        gender,
        T_DISP_gender,
        race,
        CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(),
        age,
        DAY) / 365.25) AS INT64) AS age      
    FROM
        ${ENT_person}      
    ORDER BY
        year_of_birth ASC,
        T_DISP_gender DESC,
        age ASC,
        id DESC LIMIT 35
