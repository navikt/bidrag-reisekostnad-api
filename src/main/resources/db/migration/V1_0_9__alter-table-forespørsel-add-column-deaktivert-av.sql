-- Table: forespørsel

/*
    ALTER TABLE public.forespørsel
         DROP COLUMN samtykkefrist;
 */

ALTER TABLE public.forespørsel
    ADD COLUMN deaktivert_av varchar(10);