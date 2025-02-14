
    SELECT
        name      
    FROM
        ${ENT_condition}      
    WHERE
        id IN (SELECT
            child          
        FROM
            ${HCP_condition_default}          
        WHERE
            parent IN (@val0, @val1)          
        UNION
        ALL SELECT
            @val2          
        UNION
        ALL SELECT
            @val3)
