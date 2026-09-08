import { useEffect, useState } from "react";
import { getAvailability } from "../api/appointments";
import styles from "./MobileAgenda.module.css";

export function dateValue(value) {
  const d = new Date(value);
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`;
}
export function shiftDate(value, days) { const d = new Date(value); d.setDate(d.getDate()+days); return d; }
export const timeLabel = value => new Date(value).toLocaleTimeString("en-IN", {hour:"2-digit",minute:"2-digit"});
export const dayLabel = value => new Date(value).toLocaleDateString("en-IN", {weekday:"short",day:"numeric",month:"short",year:"numeric"});

export default function MobileAgenda({ selectedDate, onDate, appointments, slots, overrides, loading, error, onRetry, onBook, onOpen, revision, modeMap, services }) {
  const [view,setView] = useState("day");
  const [offset,setOffset] = useState(0);
  const [result,setResult] = useState({items:[],loading:true,error:null});
  const [retry,setRetry] = useState(0);
  const today = dateValue(new Date());
  const anchor = new Date(`${today}T12:00:00`);
  const from = dateValue(shiftDate(anchor, view === "past" ? -30*(offset+1)+1 : 30*offset));
  const to = dateValue(shiftDate(anchor, view === "past" ? -30*offset : 30*(offset+1)-1));
  useEffect(() => {
    if (view === "day") return;
    let current = true;
    setResult({items:[],loading:true,error:null});
    getAvailability(from,to).then(data => {
      if (current) setResult({items:data.appointments || [],loading:false,error:null});
    }).catch(e => { if (current) setResult({items:[],loading:false,error:e.message}); });
    return () => { current=false; };
  },[view,from,to,revision,retry]);
  const rows = (view === "day" ? appointments : result.items.filter(a => view === "past"
    ? new Date(a.endTime) < new Date() : new Date(a.endTime) >= new Date()))
    .slice().sort((a,b)=>(new Date(a.startTime)-new Date(b.startTime))*(view === "past" ? -1 : 1));
  const busy = view === "day" ? loading : result.loading;
  const failure = view === "day" ? error : result.error;
  return <section className={styles.agenda} aria-label="Mobile appointments">
    <div className={styles.heading}><div><span className={styles.eyebrow}>Your schedule</span><h1>Appointments</h1></div></div>
    <button className="btn btn-primary" onClick={()=>onBook()}>Book appointment</button>
    <div className={styles.tabs} aria-label="Appointment view">
      {[["day","Day"],["upcoming","Upcoming"],["past","Past"]].map(([key,label])=><button key={key} aria-pressed={view===key} onClick={()=>{setView(key);setOffset(0);}}>{label}</button>)}
    </div>
    {view === "day" ? <>
      <div className={styles.dateNav}>
        <button aria-label="Previous day" onClick={()=>onDate(shiftDate(selectedDate,-1))}>‹</button>
        <label>Date<input aria-label="Agenda date" type="date" value={dateValue(selectedDate)} onChange={e=>{if(e.target.value) onDate(new Date(`${e.target.value}T12:00:00`));}}/></label>
        <button aria-label="Next day" onClick={()=>onDate(shiftDate(selectedDate,1))}>›</button>
      </div>
      <button className={styles.today} onClick={()=>onDate(new Date())}>Today</button>
    </> : <div className={styles.range}>
      <p>{dayLabel(`${from}T12:00:00`)} – {dayLabel(`${to}T12:00:00`)}</p>
      <div className={styles.tabs}><button disabled={!offset} onClick={()=>setOffset(x=>x-1)}>Nearer today</button><button onClick={()=>setOffset(x=>x+1)}>{view === "past" ? "Earlier 30 days" : "Next 30 days"}</button></div>
    </div>}
    <div aria-live="polite">
      {busy ? <p>Loading appointments…</p> : failure ? <div role="alert"><p>{failure}</p><button onClick={()=>view === "day" ? onRetry() : setRetry(x=>x+1)}>Retry</button></div> : <>
        {!rows.length && <p className={styles.empty}>No appointments {view === "day" ? "on this day" : "in this date range"}.</p>}
        <div className={styles.list}>{rows.map(a=><button className={styles.appointment} key={a.appointmentId} onClick={()=>onOpen(a)}>
          <span className={styles.date}>{dayLabel(a.startTime)} · {timeLabel(a.startTime)}–{timeLabel(a.endTime)}</span>
          <strong>{a.clientName || "Client"}</strong>
          <span>{services.find(s=>s.serviceId===a.serviceId)?.serviceType?.toLowerCase().replaceAll("_"," ") || "Session"}{modeMap[a.modeId]?.displayName ? ` · ${modeMap[a.modeId].displayName}` : ""}</span>
          <span className={styles.status}>{a.status?.toLowerCase().replaceAll("_"," ")}</span>
          <span className={styles.link}>View appointment →</span>
        </button>)}</div>
        {view === "day" && <>
          <h2>Available times</h2>
          {!slots.length ? <p className={styles.empty}>No bookable availability on this day. Choose another date.</p> : <div className={styles.slots}>{slots.slice().sort((a,b)=>new Date(a.startTime)-new Date(b.startTime)).map(s=><button key={s.slotId} onClick={()=>onBook(s)}>{timeLabel(s.startTime)}</button>)}</div>}
          {!!overrides.length && <><h2>Unavailable periods</h2>{overrides.filter(o=>!o.isAvailable).map((o,i)=><p key={o.overrideId || i}>{timeLabel(o.startTime)}–{timeLabel(o.endTime)} · {o.reason || "Unavailable"}</p>)}</>}
        </>}
      </>}
    </div>
  </section>;
}
