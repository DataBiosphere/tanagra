
    SELECT
        e.T_DISP_ethnicity AS T_DISP_ethnicity,
        e.T_DISP_gender AS T_DISP_gender,
        e.T_DISP_race AS T_DISP_race,
        CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(),
        e.age,
        DAY) / 365.25) AS INT64) AS age,
        e.biovu_sample_dna_yield AS biovu_sample_dna_yield,
        e.biovu_sample_has_plasma AS biovu_sample_has_plasma,
        e.biovu_sample_is_compromised AS biovu_sample_is_compromised,
        e.biovu_sample_is_nonshippable AS biovu_sample_is_nonshippable,
        e.ethnicity AS ethnicity,
        e.gender AS gender,
        e.has_biovu_sample AS has_biovu_sample,
        e.id AS id,
        e.person_source_value AS person_source_value,
        e.race AS race,
        e.year_of_birth AS year_of_birth 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.ENT_person AS e 
    WHERE
        e.id IN (
            SELECT
                e.person_id 
            FROM
                `verily-tanagra-dev.sd20230331_index_110623`.ENT_noteOccurrence AS e 
            WHERE
                e.note IN (
                    SELECT
                        e.id 
                    FROM
                        `verily-tanagra-dev.sd20230331_index_110623`.ENT_note AS e 
                    WHERE
                        e.id = 44814638
                )
            ) LIMIT 30
