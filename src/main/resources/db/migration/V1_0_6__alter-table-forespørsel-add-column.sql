-- Table: forespørsel

/*
    ALTER TABLE public.forespørsel
         DROP COLUMN id_journalpost;
 */

ALTER TABLE public.forespørsel
    ADD COLUMN id_journalpost varchar(255);