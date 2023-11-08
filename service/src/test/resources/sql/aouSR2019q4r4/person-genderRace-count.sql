
    SELECT
        COUNT(t.id) AS T_IDCT,
        t.gender AS gender,
        t.race AS race 
    FROM
        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_person AS t 
    GROUP BY
        gender,
        race
