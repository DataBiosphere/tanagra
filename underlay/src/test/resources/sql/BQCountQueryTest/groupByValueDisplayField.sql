
    SELECT
        COUNT(id) AS T_CTDT,
        gender      
    FROM
        ${ENT_person}      
    GROUP BY
        gender      
    ORDER BY
        T_CTDT DESC
