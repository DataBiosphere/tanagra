SELECT
    /* Can't do "*". During indexing, there's an error about person_id column being ambiguous. */
    p.person_id, p.year_of_birth, p.gender_concept_id, p.race_concept_id, p.ethnicity_concept_id,

    /* BioVU sample columns. */
    EXISTS
        (SELECT 1 FROM `victr-tanagra-test.sd_static.x_biovu_sample_status` x WHERE p.person_id = x.person_id)
                                                                                                        AS has_biovu_sample,
    x.dna_yield_ind AS biovu_sample_dna_yield,
    /* As a courtesy, convert string fields to boolean: 0 -> No, 1 -> Yes */
    CASE WHEN x.compromise_ind = '1' THEN true WHEN x.compromise_ind = '0' THEN false ELSE null END AS biovu_sample_is_compromised,
    CASE WHEN x.nonshippable_ind = '1' THEN true WHEN x.nonshippable_ind = '0' THEN false ELSE null END AS biovu_sample_is_nonshippable,
    CASE WHEN x.plasma_ind = '1' THEN true WHEN x.plasma_ind = '0' THEN false ELSE null END AS biovu_sample_has_plasma
FROM `victr-tanagra-test.sd_static.person` p
         LEFT OUTER JOIN `victr-tanagra-test.sd_static.x_biovu_sample_status` x ON p.person_id = x.person_id
