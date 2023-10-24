SELECT
    p.person_id, p.year_of_birth, p.birth_datetime, p.person_source_value,
    p.gender_concept_id, gc.concept_name AS gender_concept_name,
    p.race_concept_id, rc.concept_name AS race_concept_name,
    p.ethnicity_concept_id, ec.concept_name AS ethnicity_concept_name,

    /* Add BioVU sample columns. The way x_biovu_sample_status is created, there should be at
       most one row per person. */
    EXISTS
        (SELECT 1 FROM `sd-vumc-tanagra-test.sd_20230331.x_biovu_sample_status` x WHERE p.person_id = x.person_id AND x.dna_yield_ind > 0)
                                                                                                        AS has_biovu_sample,
    x.dna_yield_ind AS biovu_sample_dna_yield,
    /* As a courtesy, convert string fields to boolean: 0 -> No, 1 -> Yes */
    CASE WHEN x.compromise_ind = '1' THEN true WHEN x.compromise_ind = '0' THEN false ELSE null END AS biovu_sample_is_compromised,
    CASE WHEN x.nonshippable_ind = '1' THEN true WHEN x.nonshippable_ind = '0' THEN false ELSE null END AS biovu_sample_is_nonshippable,
    CASE WHEN x.plasma_ind = '1' THEN true WHEN x.plasma_ind = '0' THEN false ELSE null END AS biovu_sample_has_plasma

FROM `sd-vumc-tanagra-test.sd_20230331.person` p

LEFT JOIN `sd-vumc-tanagra-test.sd_20230331.concept` gc
ON gc.concept_id = p.gender_concept_id

LEFT JOIN `sd-vumc-tanagra-test.sd_20230331.concept` rc
ON rc.concept_id = p.race_concept_id

LEFT JOIN `sd-vumc-tanagra-test.sd_20230331.concept` ec
ON ec.concept_id = p.ethnicity_concept_id

LEFT OUTER JOIN
    (
        /* Get rid of duplicate rows in x_biovu_sample_status. For example, person
        4587323 has 11 duplicate rows. This returns just 1 row for each person. */
        WITH x_biovu_sample_status AS (
            SELECT
            *,
            ROW_NUMBER() OVER(PARTITION BY person_id) AS rn
            FROM `sd-vumc-tanagra-test.sd_20230331.x_biovu_sample_status`
        )
        SELECT * FROM x_biovu_sample_status WHERE rn = 1
    ) x
    ON p.person_id = x.person_id
