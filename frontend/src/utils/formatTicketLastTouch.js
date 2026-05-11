/** Compact “who · when” for ticket list/detail surfaces. */
export function formatTicketLastTouch(ticket) {
  if (!ticket?.updatedAt) return '—';
  const when = new Date(ticket.updatedAt).toLocaleString();
  if (ticket.lastUpdatedByName) {
    return `${ticket.lastUpdatedByName} · ${when}`;
  }
  return when;
}
