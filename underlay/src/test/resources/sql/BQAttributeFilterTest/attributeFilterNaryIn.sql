
    SELECT
        year_of_birth      
    FROM
        ${ENT_person}      
    WHERE
        CAST(FLOOR(TIMESTAMP_DIFF(@currentTimestamp3, age, DAY) / 365.25) AS INT64) IN (@val0, @val1, @val2)
