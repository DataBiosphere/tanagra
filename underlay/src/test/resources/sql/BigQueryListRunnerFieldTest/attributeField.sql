
    SELECT
        p.year_of_birth,
        p.gender,
        p.T_DISP_gender,
        p.race,
        CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(),
        p.age,
        DAY) / 365.25) AS INT64) AS age      
    FROM
        ${ENT_person} AS p      
    ORDER BY
        year_of_birth ASC,
        T_DISP_gender DESC,
        age ASC,
        p.id DESC LIMIT 35
