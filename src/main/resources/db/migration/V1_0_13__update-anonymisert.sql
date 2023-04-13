-- Oppdatere anonymisert felt for forespørsler med anonymiserte barn
update forespørsel
set anonymisert = barn.anonymisert
from barn
where forespørsel.id = barn.forespørsel_id
and forespørsel.id in (
	select b.forespørsel_id
	from barn b
	inner join forespørsel fp on fp.id = b.forespørsel_id
	where b.anonymisert is not null
	and fp.anonymisert is null
);