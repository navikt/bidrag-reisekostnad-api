-- Table: forespørsel

/*
    ALTER TABLE public.forespørsel DROP COLUMN deaktivert_av;
 */

ALTER TABLE public.forespørsel
    ADD COLUMN deaktivert_av varchar(10);