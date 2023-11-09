
    SELECT
        t.T_DISP_ethnicity AS T_DISP_ethnicity,
        t.T_DISP_gender AS T_DISP_gender,
        t.T_DISP_race AS T_DISP_race,
        CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(),
        t.age,
        DAY) / 365.25) AS INT64) AS age,
        t.biovu_sample_dna_yield AS biovu_sample_dna_yield,
        t.biovu_sample_has_plasma AS biovu_sample_has_plasma,
        t.biovu_sample_is_compromised AS biovu_sample_is_compromised,
        t.biovu_sample_is_nonshippable AS biovu_sample_is_nonshippable,
        t.ethnicity AS ethnicity,
        t.gender AS gender,
        t.has_biovu_sample AS has_biovu_sample,
        t.id AS id,
        t.person_source_value AS person_source_value,
        t.race AS race,
        t.year_of_birth AS year_of_birth 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_person AS t 
    WHERE
        t.id IN (
            SELECT
                t.person_id 
            FROM
                `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_noteOccurrence AS t 
            WHERE
                t.note IN (
                    SELECT
                        t.id 
                    FROM
                        `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_note AS t 
                    WHERE
                        t.id = 44814638
                )
            ) LIMIT 30
