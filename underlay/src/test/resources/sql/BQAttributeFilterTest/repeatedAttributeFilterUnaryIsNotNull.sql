
    SELECT
        name      
    FROM
        ${ENT_condition}      
    WHERE
        EXISTS (SELECT
            *          
        FROM
            UNNEST(['foo', 'bar', 'baz', vocabulary]))
