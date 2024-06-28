SELECT
    cast(cast(pc.criteria_meta_seq as NUMERIC) as INT64) as id,
    cast(cast(pc.criteria_meta_seq as NUMERIC) as INT64) as concept_id,
    case when pc.name like '%EXPIRED%' then regexp_extract(pc.name, '.*-(\\[EX.*)')
         when pc.is_root = true then regexp_extract(pc.name, '.*[0-9] (.*)')
         when pc.is_root = false then regexp_extract(name, '.*[0-9]-(.*)')
    end as name,
    pc.type,
    'Source' as is_standard,
    case when pc.name like '%EXPIRED%' then regexp_extract(pc.name, '(.*)-[\\(A-Z0-9].*')
         when pc.is_root = true then regexp_extract(pc.name, '(.*[0-9]) .*')
         when pc.is_root = false then regexp_extract(name, '(.*[0-9])-.*')
    end as concept_code,
    case when pc.name like '%EXPIRED%' then regexp_replace(pc.name, 'EXPIRED]-','EXPIRED] ')
         when pc.is_root = true then pc.name
         when pc.is_root = false then regexp_replace(name,'(.*[0-9])-(.*)','\\1 \\2 ')
    end as label,
FROM `${omopDataset}.icd_criteria` pc
