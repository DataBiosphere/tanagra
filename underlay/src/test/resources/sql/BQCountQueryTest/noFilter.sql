
    SELECT
        COUNT(id) AS T_CTDT,
        year_of_birth      
    FROM
        ${ENT_person}      
    GROUP BY
        year_of_birth      
    ORDER BY
        T_CTDT DESC
