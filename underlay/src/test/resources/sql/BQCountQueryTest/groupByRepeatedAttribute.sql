
    SELECT
        COUNT(id) AS T_CTDT,
        FLATTENED_vocabulary      
    FROM
        ${ENT_condition}      
    CROSS JOIN
        UNNEST(['foo', 'bar', 'baz', vocabulary]) AS FLATTENED_vocabulary      
    GROUP BY
        FLATTENED_vocabulary      
    ORDER BY
        T_CTDT DESC
