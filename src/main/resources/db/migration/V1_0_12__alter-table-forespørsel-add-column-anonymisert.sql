-- Table: Forespørsel

/*
    ALTER TABLE public.forespørsel
        DROP COLUMN anonymisert;
 */

ALTER TABLE public.forespørsel ADD COLUMN anonymisert timestamp without time zone;
