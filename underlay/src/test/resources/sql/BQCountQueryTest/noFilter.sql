
    SELECT
        COUNT(id) AS T_IDCT,
        year_of_birth      
    FROM
        ${ENT_person}      
    GROUP BY
        year_of_birth
