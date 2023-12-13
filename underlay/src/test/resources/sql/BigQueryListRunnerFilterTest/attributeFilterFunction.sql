
    SELECT
        p.year_of_birth      
    FROM
        ${ENT_person} AS p      
    WHERE
        CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), p.age, DAY) / 365.25) AS INT64) NOT IN (
            @val,@val0         
        )
