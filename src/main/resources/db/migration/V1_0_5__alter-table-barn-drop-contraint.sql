-- Table: barn

/*
    ALTER TABLE public.barn
        ADD CONSTRAINT uk_barn_personident UNIQUE (personident),
*/

ALTER TABLE public.barn
    DROP CONSTRAINT uk_barn_personident;