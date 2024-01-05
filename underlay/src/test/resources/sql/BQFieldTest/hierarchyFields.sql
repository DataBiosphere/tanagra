
    SELECT
        (T_PATH_default IS NOT NULL) AS T_ISMEM_default,
        (T_PATH_default IS NOT NULL          
        AND T_PATH_default='') AS T_ISRT_default,
        T_NUMCH_default,
        T_PATH_default      
    FROM
        ${ENT_condition}      
    ORDER BY
        T_NUMCH_default DESC
