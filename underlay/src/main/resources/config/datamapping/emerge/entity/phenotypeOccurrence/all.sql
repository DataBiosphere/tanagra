SELECT
    ROW_NUMBER() OVER () AS row_id,
    p.IND_SEQ AS person_id,
    pc.concept_id,
    pc.concept_name,
    pc.concept_code,
    pc.concept_code AS standard_code,
    p.decade_birth
FROM (
      SELECT
         IND_SEQ,
         cast(parse_numeric(decade_birth) as INT64) as decade_birth,
         REGEXP_EXTRACT(col_name,'CASE_CONTROL_(.*)') AS code,
         code_val,
      FROM `${omopDataset}.phenotypes`
            UNPIVOT(code_val FOR col_name IN (
                CASE_CONTROL_AAA,
                CASE_CONTROL_ACEI,
                CASE_CONTROL_ADHD,
                CASE_CONTROL_AMD,
                CASE_CONTROL_APPENDICITIS,
                CASE_CONTROL_ASTHMA,
                CASE_CONTROL_ATOPICDERMATITIS,
                CASE_CONTROL_AUTISM,
                CASE_CONTROL_BPH,
                CASE_CONTROL_CAAD,
                CASE_CONTROL_CAMRSA,
                CASE_CONTROL_CATARACT,
                CASE_CONTROL_CDIFF,
                CASE_CONTROL_CHILDHOODOBESITY,
                CASE_CONTROL_CKD,
                CASE_CONTROL_CKDT2D,
                CASE_CONTROL_CKDT2DHTN,
                CASE_CONTROL_COLONPOLYPS,
                CASE_CONTROL_CRF,
                CASE_CONTROL_DEMENTIA,
                CASE_CONTROL_DIV,
                CASE_CONTROL_DR,
                CASE_CONTROL_EXTREMEOBESITY,
                CASE_CONTROL_GERD,
                CASE_CONTROL_GLAUCOMA,
                CASE_CONTROL_HEIGHT,
                CASE_CONTROL_HF,
                CASE_CONTROL_HYPOTHYROIDISM,
                CASE_CONTROL_LIPIDS,
                CASE_CONTROL_OCULARHTN,
                CASE_CONTROL_PAD,
                CASE_CONTROL_QRS,
                CASE_CONTROL_RBC,
                CASE_CONTROL_REMISSIONDIABETES,
                CASE_CONTROL_RESHYP,
                CASE_CONTROL_STATINSMACE,
                CASE_CONTROL_T2D,
                CASE_CONTROL_VTE,
                CASE_CONTROL_WBC,
                CASE_CONTROL_ZOSTER)
            )
WHERE code_val != 'NA' AND regexp_contains(decade_birth, '\\d+') ) p
JOIN (
    SELECT
        criteria_meta_seq AS concept_id,
        IF (STARTS_WITH(label, 'Phenotype_'), REGEXP_EXTRACT(label,'Phenotype_(.* - C.*)_'),label) AS concept_name,
        CASE
            WHEN is_leaf = TRUE THEN CONCAT(
                    CASE
                     WHEN REGEXP_CONTAINS(label, 'Resistant Hypertension') THEN 'RESHYP'
                     ELSE UPPER(REGEXP_EXTRACT(label,'Phenotype_.* - C.*_(.*)_C.*')) END
                     , '-', UPPER(REGEXP_EXTRACT(label,'Phenotype_.* - C.*_.*_(C?\\d+)')) )
            ELSE CASE
                 WHEN REGEXP_CONTAINS(label, 'Resistant Hypertension') THEN 'RESHYP'
                 ELSE UPPER(REGEXP_REPLACE(label,' ',''))END
            END AS concept_code,
        CASE
            WHEN REGEXP_CONTAINS(label, 'Resistant Hypertension') THEN 'RESHYP'
            ELSE
                IF (STARTS_WITH(label, 'Phenotype_'), UPPER(REGEXP_EXTRACT(label,'Phenotype_.* - C.*_(.*)_C.*')),UPPER(REGEXP_REPLACE(label,' ','')))
            END
            AS c_code,
        IF (STARTS_WITH(label, 'Phenotype_'), UPPER(REGEXP_EXTRACT(label,'Phenotype_.* - C.*_.*_(C?\\d+)')),UPPER(REGEXP_REPLACE(label,' ',''))) AS c_code_val
    FROM `${omopDataset}.phenotype_criteria`
    ) pc ON p.code = pc.c_code
         AND p.code_val = pc.c_code_val
