
    SELECT
        COUNT(id) AS T_CTDT,
        year_of_birth      
    FROM
        ${ENT_person}      
    WHERE
        gender != @val0      
    GROUP BY
        year_of_birth      
    ORDER BY
        T_CTDT DESC
