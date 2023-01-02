-- Table: Barn

/*
    ALTER TABLE public.barn
        DROP COLUMN fødselsdato;
 */

ALTER TABLE public.barn ADD COLUMN anonymisert timestamp without time zone;
