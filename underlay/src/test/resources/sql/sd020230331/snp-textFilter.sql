
    SELECT
        t.id AS id,
        t.name AS name 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_snp AS t 
    WHERE
        REGEXP_CONTAINS(UPPER(t.name), UPPER('RS1292')) LIMIT 30
