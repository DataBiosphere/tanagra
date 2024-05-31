
    SELECT
        COUNT(id) AS T_IDCT,
        gender      
    FROM
        ${ENT_person}      
    GROUP BY
        gender      
    ORDER BY
        T_IDCT DESC
