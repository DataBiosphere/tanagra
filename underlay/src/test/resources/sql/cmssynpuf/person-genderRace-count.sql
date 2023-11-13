
    SELECT
        COUNT(e.id) AS T_IDCT,
        e.gender AS gender,
        e.race AS race 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_person AS e 
    GROUP BY
        gender,
        race
