-- Table: forespørsel

/*
    ALTER TABLE public.barn
        DROP COLUMN fødselsdato;
 */

ALTER TABLE public.barn ADD COLUMN fødselsdato date;
