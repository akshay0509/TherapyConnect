// The backend reserves whole 30-minute blocks for a session. Only offer a start
// when every block in that footprint is free, including the last one.
export function fittingRescheduleStarts(freeSlots, appointmentStart, appointmentEnd) {
  const blockMs = 30 * 60 * 1000;
  const durationMs = new Date(appointmentEnd).getTime() - new Date(appointmentStart).getTime();
  if (!Number.isFinite(durationMs) || durationMs <= 0) return [];
  const blocks = Math.ceil(durationMs / blockMs);
  if (blocks > freeSlots.length) return [];
  const starts = new Set(freeSlots
    .filter(slot => new Date(slot.endTime) - new Date(slot.startTime) >= blockMs)
    .map(slot => new Date(slot.startTime).getTime()));
  return freeSlots.filter(slot => {
    const start = new Date(slot.startTime).getTime();
    for (let i = 0; i < blocks; i++) {
      if (!starts.has(start + i * blockMs)) return false;
    }
    return true;
  });
}
