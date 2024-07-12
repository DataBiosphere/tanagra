SELECT
    pc.criteria_meta_seq AS id,
    pc.criteria_meta_seq AS concept_id,
    IF(STARTS_WITH(pc.label, 'Phenotype_'), REGEXP_EXTRACT(label,'Phenotype_(.* - C.*)_'),label) as name,
    'PHENOTYPE' as type,
    'Source' as is_standard,
    CASE WHEN pc.is_leaf = true THEN
         CONCAT(
             CASE WHEN REGEXP_CONTAINS(pc.label, 'Resistant Hypertension') THEN 'RESHYP'
             ELSE UPPER(REGEXP_EXTRACT(pc.label,'Phenotype_.* - C.*_(.*)_C.*')) END,
             '-',
             UPPER(REGEXP_EXTRACT(pc.label,'Phenotype_.* - C.*_.*_(C?\\d+)'))
         )
     ELSE
         CASE WHEN REGEXP_CONTAINS(pc.label, 'Resistant Hypertension') THEN 'RESHYP'
         ELSE UPPER(REGEXP_REPLACE(pc.label,' ','')) END
    END as concept_code,
    IF(STARTS_WITH(pc.label, 'Phenotype_'), REGEXP_EXTRACT(pc.label,'Phenotype_(.*)'),pc.label) as label
FROM `${omopDataset}.phenotype_criteria` pc
