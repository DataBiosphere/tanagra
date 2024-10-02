
    SELECT
        st.year_of_birth,
        st.gender_concept_id,
        dt0.concept_name AS T_DISP_gender,
        st.race_concept_id,
        st.birth_datetime,
        st.person_id,
        dt0.concept_name AS T_DISP_genderSuppressed,
        st.person_source_value,
        st.ethnicity_concept_id AS T_DISP_ethnicityNoDisplayJoin      
    FROM
        ${person} AS st      
    LEFT JOIN
        ${concept} AS dt0              
            ON dt0.concept_id = st.gender_concept_id      
    WHERE
        person_source_value IS NOT NULL
