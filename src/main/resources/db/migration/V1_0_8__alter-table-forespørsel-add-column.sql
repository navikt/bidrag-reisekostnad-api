-- Table: forespørsel

/*
    ALTER TABLE public.forespørsel
         DROP COLUMN samtykkefrist;
 */

ALTER TABLE public.forespørsel
    ADD COLUMN samtykkefrist date;