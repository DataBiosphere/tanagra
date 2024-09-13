
    SELECT
        name      
    FROM
        ${ENT_condition}      
    WHERE
        EXISTS (SELECT
            *          
        FROM
            UNNEST(['foo', 'bar', 'baz', vocabulary]) AS flattened          
        WHERE
            flattened IN (@val0, @val1, @val2))
