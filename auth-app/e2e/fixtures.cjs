const TODAY = '2026-09-07';
const stamp = (day, time) => `${day}T${time}:00`;
async function mockApi(page) {
  await page.clock.install({ time: new Date(`${TODAY}T09:00:00`) });
  const appointments = [
    { appointmentId: 'a1', clientId: 'c1', clientName: 'Alex Morgan', status: 'CONFIRMED', startTime: stamp(TODAY, '11:00'), endTime: stamp(TODAY, '12:00'), modeId: 'm1', serviceId: 's1' },
    { appointmentId: 'a2', clientId: 'c1', clientName: 'Alex Morgan', status: 'COMPLETED', startTime: stamp('2026-08-25', '11:00'), endTime: stamp('2026-08-25', '12:00'), modeId: 'm1', serviceId: 's1' },
    { appointmentId: 'a3', clientId: 'c1', clientName: 'Alex Morgan', status: 'SCHEDULED', startTime: stamp('2026-09-21', '11:00'), endTime: stamp('2026-09-21', '12:00'), modeId: 'm1', serviceId: 's1' },
  ];
  const calls = []; let conflict = false;
  const token = `x.${Buffer.from(JSON.stringify({ sub:'demo', authorities:['THERAPIST'], therapistId:'t1', exp: new Date(`${TODAY}T10:00:00`).getTime()/1000 })).toString('base64url')}.x`;
  await page.route('**/*', async route => {
    const req = route.request(), url = new URL(req.url()), p = url.pathname;
    if (url.port === '5173' && !p.startsWith('/auth/')) return route.continue();
    if (!['localhost','127.0.0.1'].includes(url.hostname)) return route.abort();
    calls.push({ path:p, query:url.search, method:req.method(), body:req.postDataJSON() });
    let data = {};
    if (p === '/auth/refresh' || p === '/auth/login') data = { token };
    else if (p.includes('therapistProfile')) data = { firstName:'Demo', lastName:'Therapist' };
    else if (p.endsWith('/therapist-services')) data = [{ serviceId:'s1', serviceType:'INDIVIDUAL', duration:60, isActive:true }];
    else if (p.endsWith('/delivery-modes')) data = [{ serviceId:'s1', modeId:'m1', displayName:'Online', modeType:'ONLINE', price:1000, isActive:true }];
    else if (p === '/therapist/clients') data = [{ clientId:'c1', clientName:'Alex Morgan' }];
    else if (p.startsWith('/client/get/')) data = { clientId:'c1', clientName:'Alex Morgan', sessionFee:1000 };
    else if (p.includes('intakes')) data = [];
    else if (p === '/appointment/editor-view') {
      const from = url.searchParams.get('fromDate'), to = url.searchParams.get('toDate');
      const slots = [];
      for (let d = new Date(`${from}T12:00:00`); d <= new Date(`${to}T12:00:00`); d.setDate(d.getDate()+1)) {
        const day = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
        for (const time of ['13:00','13:30','14:00','14:30']) {
          const end = time.endsWith('00') ? time.replace('00','30') : `${Number(time.slice(0,2))+1}:00`;
          const startTime = stamp(day,time), endTime = stamp(day,end);
          slots.push({ slotId:`${day}_${time}`, therapistId:'t1', startTime, endTime, slotStatus: appointments.some(a=>!['CANCELLED','ABANDONED'].includes(a.status) && a.startTime < endTime && a.endTime > startTime) ? 'BOOKED' : 'AVAILABLE' });
        }
      }
      data = { slots, appointments:appointments.filter(a=>a.startTime.slice(0,10)>=from && a.startTime.slice(0,10)<=to), overrides:[] };
    } else if (p === '/appointment/create-appointment') {
      if (conflict) { conflict=false; return route.fulfill({ status:409, json:{message:'SLOT_ALREADY_BOOKED'} }); }
      const b=req.postDataJSON(), [day,time]=b.slotId.split('_');
      appointments.push({ appointmentId:'new', clientId:b.clientId, clientName:b.clientName, modeId:b.modeId, status:'SCHEDULED', startTime:stamp(day,time), endTime:stamp(day,`${Number(time.slice(0,2))+1}:${time.slice(3)}`) });
      data={appointmentId:'new'};
    } else if (p === '/appointment/update-appointment') {
      const b=req.postDataJSON(); Object.assign(appointments.find(a=>a.appointmentId===b.appointmentId),{status:b.status});
    } else if (p === '/appointment/reschedule-appointment') {
      const b=req.postDataJSON(), [day,time]=b.newSlotId.split('_');
      Object.assign(appointments.find(a=>a.appointmentId===b.appointmentId), {startTime:stamp(day,time),endTime:stamp(day,`${Number(time.slice(0,2))+1}:${time.slice(3)}`),status:'RESCHEDULED'});
    } else if (p.includes('/payments/')) return route.fulfill({status:404,json:{}});
    return route.fulfill({ status:200, json:data });
  });
  return { calls, appointments, conflictNext:()=>{conflict=true;} };
}
module.exports = { mockApi };
