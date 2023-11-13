
    SELECT
        e.id AS id,
        e.name AS name 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.ENT_genotyping AS e 
    WHERE
        REGEXP_CONTAINS(UPPER(e.name), UPPER('Illumina')) LIMIT 30
