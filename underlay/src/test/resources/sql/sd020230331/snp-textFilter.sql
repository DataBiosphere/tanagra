
    SELECT
        e.id AS id,
        e.name AS name 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.ENT_snp AS e 
    WHERE
        REGEXP_CONTAINS(UPPER(e.name), UPPER('RS1292')) LIMIT 30
