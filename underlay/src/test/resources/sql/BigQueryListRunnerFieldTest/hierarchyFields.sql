
    SELECT
        (c.T_PATH_default IS NOT NULL) AS T_ISMEM_default,
        (c.T_PATH_default IS NOT NULL          
        AND c.T_PATH_default='') AS T_ISRT_default,
        c.T_NUMCH_default,
        c.T_PATH_default      
    FROM
        ${ENT_condition} AS c      
    ORDER BY
        T_NUMCH_default DESC
