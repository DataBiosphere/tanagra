
    SELECT
        COUNT(p.id) AS T_IDCT      
    FROM
        ${ENT_person} AS p      
    ORDER BY
        T_IDCT DESC
